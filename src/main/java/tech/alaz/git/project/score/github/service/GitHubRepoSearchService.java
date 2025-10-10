package tech.alaz.git.project.score.github.service;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tech.alaz.git.project.score.api.controller.dto.ScoredGitHubRepoSearchResponseDto;
import tech.alaz.git.project.score.domain.dto.ScoredGitProjectDto;
import tech.alaz.git.project.score.domain.score.strategy.ScoringStrategy;
import tech.alaz.git.project.score.github.client.graphql.GithubSearchGraphQlResponseMapper;
import tech.alaz.git.project.score.github.client.graphql.GraphQlUtil;
import tech.alaz.git.project.score.github.client.graphql.dto.GitHubGraphQlSearchResponseDto;
import tech.alaz.git.project.score.github.client.graphql.exception.GitHubGraphQlClientException;
import tech.alaz.git.project.score.github.client.rest.GitHubSearchRestResponseMapper;
import tech.alaz.git.project.score.github.client.rest.exception.GitHubRestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static tech.alaz.git.project.score.github.client.rest.GitHubRestClientConfig.FORKS;
import static tech.alaz.git.project.score.github.client.rest.GitHubRestClientConfig.STARS;

@Service
public class GitHubRepoSearchService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubRepoSearchService.class);

    private final HttpGraphQlClient gitHubGraphQlClient;
    private final WebClient gitHubRestWebClient;
    private final String githubRepoSearchQueryName;
    private final int maxPageSize;

    public GitHubRepoSearchService(
            HttpGraphQlClient gitHubGraphQLClient, WebClient gitHubRestWebClient, @Value("${git-project-score.max-search-page-size:5}") int maxPageSize) {
        this.gitHubGraphQlClient = gitHubGraphQLClient;
        this.gitHubRestWebClient = gitHubRestWebClient;
        this.githubRepoSearchQueryName = GraphQlUtil.loadGraphQLQuery("GitHubReposSearchQuery.graphql");
        this.maxPageSize = maxPageSize;
    }

    public Mono<ScoredGitHubRepoSearchResponseDto> searchAndScoreGitHubRepos(
            String searchTermInName,
            String language,
            LocalDate createdAfter,
            Integer first,
            String after,
            ScoringStrategy scoringStrategy) {

        logger.info("Starting GitHub repository search and scoring: searchTerm={}, language={}, createdAfter={}, first={}, scoringStrategy={}",
                searchTermInName, language, createdAfter, first, scoringStrategy.getClass().getSimpleName());

        Mono<GitHubGraphQlSearchResponseDto> gitHubGraphQlSearchResponseDtoMono = searchGitHubReposViaGraphQl(searchTermInName, language, createdAfter, first, after)
                .doOnSuccess(response -> logger.debug("GraphQL search returned {} repositories", response.totalResultCount()))
                .doOnError(error -> logger.error("GraphQL search failed", error))
                .onErrorStop(); // TODO check if there is better alternative

        if (scoringStrategy.isRelativeToOtherProjects()) {
            logger.debug("Using relative scoring strategy - fetching max stars and forks");

            Mono<Integer> maxForkCountMono = searchGitHubRepoViaRestApiForMaxOrMin(searchTermInName, language, createdAfter, 1, 0, FORKS, true)
                    .map(gitHubGraphQlSearchResponseDto -> {
                        val repositories = gitHubGraphQlSearchResponseDto.repositories();
                        int maxForks = repositories.isEmpty() ? 0 : repositories.getFirst().forkCount();
                        logger.debug("Max fork count: {}", maxForks);
                        return maxForks;
                    })
                    .onErrorResume(e -> {
                        logger.warn("Failed to fetch max fork count, using default value", e);
                        return Mono.just(-1);
                    });

            Mono<Integer> maxStarGazersCountMono = searchGitHubRepoViaRestApiForMaxOrMin(searchTermInName, language, createdAfter, 1, 0, STARS, true)
                    .map(gitHubGraphQlSearchResponseDto -> {
                        val repositories = gitHubGraphQlSearchResponseDto.repositories();
                        int maxStars = repositories.isEmpty() ? 0 : repositories.getFirst().stargazerCount();
                        logger.debug("Max star count: {}", maxStars);
                        return maxStars;
                    })
                    .onErrorResume(e -> {
                        logger.warn("Failed to fetch max star count, using default value", e);
                        return Mono.just(-1);
                    });

            return Mono.zip(gitHubGraphQlSearchResponseDtoMono, maxForkCountMono, maxStarGazersCountMono)
                    .map(tuple3 -> {
                        logger.debug("Mapping and scoring {} repositories with maxForks={}, maxStars={}",
                                tuple3.getT1().repositories().size(), tuple3.getT2(), tuple3.getT3());
                        return createScoredGitHubRepoSearchResponseDto(scoringStrategy, tuple3.getT1(), tuple3.getT2(), tuple3.getT3());
                    })
                    .doOnSuccess(result -> logger.info("Successfully scored {} repositories", result.githubRepositories().size()));

        } else {
            logger.error("Non-relative scoring strategy not yet implemented");
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    private ScoredGitHubRepoSearchResponseDto createScoredGitHubRepoSearchResponseDto(
            ScoringStrategy scoringStrategy,
            GitHubGraphQlSearchResponseDto gitHubGraphQlSearchResponseDto,
            int maxForkCount,
            int maxStarGazersCount) {

        return new ScoredGitHubRepoSearchResponseDto(
                gitHubGraphQlSearchResponseDto.totalResultCount(),
                maxStarGazersCount,
                maxForkCount,
                gitHubGraphQlSearchResponseDto.pageInfo(),
                gitHubGraphQlSearchResponseDto.repositories().stream()
                        .map(githubRepositoryDto -> new ScoredGitProjectDto(githubRepositoryDto,
                                scoringStrategy.calculateScore(githubRepositoryDto, gitHubGraphQlSearchResponseDto.totalResultCount(), maxForkCount, maxStarGazersCount))
                        )
                        .toList());
    }

    /**
     * Search for repositories on GitHub using GraphQL with creation date and language filters
     *
     * @param language         Primary programming language (e.g., "Java", "Python")
     * @param createdAfter     Earliest creation date (format: YYYY-MM-DD)
     * @param first            Number of results to return (max 100)
     * @param after            Cursor for pagination
     * @param searchTermInName Repository name or partial name to search for
     * @return Mono of GitHubSearchResponseDto containing the search results
     */
    public Mono<GitHubGraphQlSearchResponseDto> searchGitHubReposViaGraphQl(
            String searchTermInName,
            String language,
            LocalDate createdAfter,
            Integer first,
            String after) {

        StringBuilder queryBuilder = new StringBuilder();

        if (searchTermInName != null && !searchTermInName.isEmpty()) {
            queryBuilder.append(searchTermInName).append(" in:name");
        }

        if (language != null && !language.isEmpty()) {
            if (!queryBuilder.isEmpty()) queryBuilder.append(" ");
            queryBuilder.append("language:").append(language);
        }

        if (createdAfter != null) {
            String createdAfterStr = createdAfter.format(DateTimeFormatter.ISO_LOCAL_DATE);
            if (!queryBuilder.isEmpty()) queryBuilder.append(" ");
            queryBuilder.append("created:>=").append(createdAfterStr);
        }

        String searchQuery = queryBuilder.toString();
        if (searchQuery.isEmpty()) {
            return Mono.error(
                    new GitHubGraphQlClientException("At least one search parameter must be provided"));
        }

        return gitHubGraphQlClient
                .document(githubRepoSearchQueryName)
                .variable("query", searchQuery)
                .variable("first", first != null ? first : maxPageSize)
                .variable("after", after)
                .retrieve("search")
                .toEntity(Map.class)
                .map(GithubSearchGraphQlResponseMapper::parseResponse);
    }

    /**
     * Search repositories using REST API (supports sorting by stars) This is the alternative to the
     * GraphQL search when server-side sorting is needed
     * CAUTION: Because of REST API doesn't use cursor-based pagination like GraphQL,
     * the current use of this method is not suitable for REST pagination.
     *
     * @param language         Primary programming language
     * @param createdAfter     Earliest creation date
     * @param first            Number of results to return (max 100)
     * @param page             Page number for pagination
     * @param searchTermInName Repository name search term
     * @param sortBy           Sort field: "stars", "forks"
     * @param sortDescending   Sort direction
     * @return Mono of GitHubSearchResponseDto
     */
    public Mono<GitHubGraphQlSearchResponseDto> searchGitHubRepoViaRestApiForMaxOrMin(
            String searchTermInName,
            String language,
            LocalDate createdAfter,
            Integer first,
            Integer page,
            String sortBy,
            boolean sortDescending) {

        StringBuilder queryBuilder = new StringBuilder();

        if (searchTermInName != null && !searchTermInName.isEmpty()) {
            queryBuilder.append(searchTermInName).append(" in:name");
        }

        if (language != null && !language.isEmpty()) {
            if (!queryBuilder.isEmpty()) queryBuilder.append(" ");
            queryBuilder.append("language:").append(language);
        }

        if (createdAfter != null && !createdAfter.isAfter(LocalDate.now())) {
            String createdAfterStr = createdAfter.format(DateTimeFormatter.ISO_LOCAL_DATE);
            if (!queryBuilder.isEmpty()) queryBuilder.append(" ");
            queryBuilder.append("created:>=").append(createdAfterStr);
        }

        String searchQuery = queryBuilder.toString();
        if (searchQuery.isEmpty()) {
            return Mono.error(
                    new GitHubRestClientException("At least one search parameter must be provided"));
        }

        return gitHubRestWebClient
                .get()
                .uri(
                        uriBuilder -> {
                            uriBuilder
                                    .path("/search/repositories")
                                    .queryParam("q", searchQuery)
                                    .queryParam("per_page", first != null ? first : maxPageSize);

                            if (page != null) {
                                uriBuilder.queryParam("page", page);
                            }

                            // Add sort parameters if provided
                            if (sortBy != null && !sortBy.isEmpty()) {
                                String sort =
                                        switch (sortBy.toLowerCase()) {
                                            case STARS -> "stars";
                                            case FORKS -> "forks";
                                            default -> null;
                                        };

                                if (sort != null) {
                                    uriBuilder.queryParam("sort", sort);
                                    uriBuilder.queryParam(
                                            "order", sortDescending ? "desc" : "asc");
                                }
                            }

                            return uriBuilder.build();
                        })
                .retrieve()
                .bodyToMono(Map.class)
                .map(GitHubSearchRestResponseMapper::parseRestApiResponse);
    }
}
