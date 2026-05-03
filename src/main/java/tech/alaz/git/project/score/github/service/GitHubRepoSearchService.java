package tech.alaz.git.project.score.github.service;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static tech.alaz.git.project.score.github.client.rest.GitHubRestClientConfig.FORKS;
import static tech.alaz.git.project.score.github.client.rest.GitHubRestClientConfig.STARS;

@Service
public class GitHubRepoSearchService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubRepoSearchService.class);

    private final HttpSyncGraphQlClient gitHubGraphQlClient;
    private final RestClient gitHubRestWebClient;
    private final String githubRepoSearchQueryName;
    private final int maxPageSize;

    public GitHubRepoSearchService(
            HttpSyncGraphQlClient gitHubGraphQLClient, RestClient gitHubRestWebClient, @Value("${git-project-score.max-search-page-size:5}") int maxPageSize) {
        this.gitHubGraphQlClient = gitHubGraphQLClient;
        this.gitHubRestWebClient = gitHubRestWebClient;
        this.githubRepoSearchQueryName = GraphQlUtil.loadGraphQLQuery("GitHubReposSearchQuery.graphql");
        this.maxPageSize = maxPageSize;
    }

    public ScoredGitHubRepoSearchResponseDto searchAndScoreGitHubRepos(
            String searchTermInName,
            String language,
            LocalDate createdAfter,
            Integer first,
            String after,
            ScoringStrategy scoringStrategy) {

        logger.info("Starting GitHub repository search and scoring: searchTerm={}, language={}, createdAfter={}, first={}, scoringStrategy={}",
                searchTermInName, language, createdAfter, first, scoringStrategy.getClass().getSimpleName());

        if (scoringStrategy.isRelativeToOtherProjects()) {
            logger.debug("Using relative scoring strategy - fetching max stars and forks");

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var graphqlFuture = CompletableFuture.supplyAsync(
                        () -> {
                            var result = searchGitHubReposViaGraphQl(searchTermInName, language, createdAfter, first, after);
                            logger.debug("GraphQL search returned {} repositories", result.totalResultCount());
                            return result;
                        }, executor);

                var maxForksFuture = CompletableFuture.supplyAsync(
                        () -> fetchMaxForkCount(searchTermInName, language, createdAfter), executor);

                var maxStarsFuture = CompletableFuture.supplyAsync(
                        () -> fetchMaxStarCount(searchTermInName, language, createdAfter), executor);

                try {
                    CompletableFuture.allOf(graphqlFuture, maxForksFuture, maxStarsFuture).join();
                } catch (CompletionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException re) throw re;
                    throw new RuntimeException(cause);
                }

                GitHubGraphQlSearchResponseDto graphqlResult = graphqlFuture.join();
                int maxForks = maxForksFuture.join();
                int maxStars = maxStarsFuture.join();

                logger.debug("Mapping and scoring {} repositories with maxForks={}, maxStars={}",
                        graphqlResult.repositories().size(), maxForks, maxStars);

                ScoredGitHubRepoSearchResponseDto result = createScoredGitHubRepoSearchResponseDto(scoringStrategy, graphqlResult, maxForks, maxStars);
                logger.info("Successfully scored {} repositories", result.githubRepositories().size());
                return result;
            }
        } else {
            logger.error("Non-relative scoring strategy not yet implemented");
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    private int fetchMaxForkCount(String searchTermInName, String language, LocalDate createdAfter) {
        try {
            val result = searchGitHubRepoViaRestApiForMaxOrMin(searchTermInName, language, createdAfter, 1, 0, FORKS, true);
            int maxForks = result.repositories().isEmpty() ? 0 : result.repositories().getFirst().forkCount();
            logger.debug("Max fork count: {}", maxForks);
            return maxForks;
        } catch (Exception e) {
            logger.warn("Failed to fetch max fork count, using default value", e);
            return -1;
        }
    }

    private int fetchMaxStarCount(String searchTermInName, String language, LocalDate createdAfter) {
        try {
            val result = searchGitHubRepoViaRestApiForMaxOrMin(searchTermInName, language, createdAfter, 1, 0, STARS, true);
            int maxStars = result.repositories().isEmpty() ? 0 : result.repositories().getFirst().stargazerCount();
            logger.debug("Max star count: {}", maxStars);
            return maxStars;
        } catch (Exception e) {
            logger.warn("Failed to fetch max star count, using default value", e);
            return -1;
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
     * @return GitHubGraphQlSearchResponseDto containing the search results
     */
    public GitHubGraphQlSearchResponseDto searchGitHubReposViaGraphQl(
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
            throw new GitHubGraphQlClientException("At least one search parameter must be provided");
        }

        Map result = gitHubGraphQlClient
                .document(githubRepoSearchQueryName)
                .variable("query", searchQuery)
                .variable("first", first != null ? first : maxPageSize)
                .variable("after", after)
                .retrieveSync("search")
                .toEntity(Map.class);

        return GithubSearchGraphQlResponseMapper.parseResponse(result);
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
     * @return GitHubGraphQlSearchResponseDto
     */
    public GitHubGraphQlSearchResponseDto searchGitHubRepoViaRestApiForMaxOrMin(
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
            throw new GitHubRestClientException("At least one search parameter must be provided");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = gitHubRestWebClient
                .get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .path("/search/repositories")
                            .queryParam("q", searchQuery)
                            .queryParam("per_page", first != null ? first : maxPageSize);

                    if (page != null) {
                        uriBuilder.queryParam("page", page);
                    }

                    if (sortBy != null && !sortBy.isEmpty()) {
                        String sort = switch (sortBy.toLowerCase()) {
                            case STARS -> "stars";
                            case FORKS -> "forks";
                            default -> null;
                        };

                        if (sort != null) {
                            uriBuilder.queryParam("sort", sort);
                            uriBuilder.queryParam("order", sortDescending ? "desc" : "asc");
                        }
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .body(Map.class);

        return GitHubSearchRestResponseMapper.parseRestApiResponse(response);
    }
}