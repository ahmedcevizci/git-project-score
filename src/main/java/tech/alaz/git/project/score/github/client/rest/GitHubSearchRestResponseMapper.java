package tech.alaz.git.project.score.github.client.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.github.client.graphql.dto.GitHubGraphQlSearchResponseDto;
import tech.alaz.git.project.score.github.client.graphql.dto.PageInfoDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GitHubSearchRestResponseMapper {

    private static final Logger logger = LoggerFactory.getLogger(GitHubSearchRestResponseMapper.class);

    /**
     * Parse REST API response to our DTO format
     * CAUTION: Because of REST API doesn't use cursor-based pagination like GraphQL, current use of this method is not suitable for REST pagination.
     */
    @SuppressWarnings("unchecked")
    public static GitHubGraphQlSearchResponseDto parseRestApiResponse(Map<String, Object> response) {
        logger.debug("Parsing REST API response");

        if (response == null) {
            logger.error("Received null response from REST API");
            return new GitHubGraphQlSearchResponseDto(0, createDummyEmptyPageInfo(), List.of());
        }

        Integer totalCount = (Integer) response.get("total_count");
        Boolean incompleteResults = (Boolean) response.get("incomplete_results");
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        logger.debug("REST API response: totalCount={}, incompleteResults={}, items={}",
                totalCount, incompleteResults, items != null ? items.size() : 0);

        if (items == null) {
            logger.warn("No items found in REST API response");
            return new GitHubGraphQlSearchResponseDto(
                    totalCount != null ? totalCount : 0,
                    createDummyEmptyPageInfo(),
                    List.of()
            );
        }

        // Map REST API items to our DTO
        List<GithubRepositoryDto> repositories = items.stream()
                .filter(Objects::nonNull)
                .map(GitHubSearchRestResponseMapper::mapRestApiItemToGithubRepositoryDto)
                .toList();

        logger.debug("Successfully parsed {} repositories from REST API response", repositories.size());

        // TODO Because of REST API doesn't use cursor-based pagination like GraphQL, current use of this method is not suitable for REST pagination.
        PageInfoDto pageInfo = createDummyEmptyPageInfo();

        return new GitHubGraphQlSearchResponseDto(
                totalCount != null ? totalCount : 0,
                pageInfo,
                repositories
        );
    }

    @SuppressWarnings("unchecked")
    private static GithubRepositoryDto mapRestApiItemToGithubRepositoryDto(Map<String, Object> item) {
        String repoName = (String) item.get("name");
        logger.debug("Mapping REST API item for repository: {}", repoName);

        // Parse language
        String primaryLanguage = (String) item.get("language");

        // Parse license
        Map<String, Object> licenseMap = (Map<String, Object>) item.get("license");
        String licenceName = null;
        if (licenseMap != null) {
            licenceName = (String) licenseMap.get("name");
        }

        return new GithubRepositoryDto(
                repoName,
                (String) item.get("description"),
                (String) item.get("html_url"),
                primaryLanguage,
                parseInstant((String) item.get("created_at")),
                parseInstant((String) item.get("updated_at")),
                parseInstant((String) item.get("pushed_at")),
                (Integer) item.get("stargazers_count"),
                (Integer) item.get("watchers_count"),
                (Integer) item.get("forks_count"),
                (Boolean) item.get("fork"),// CAUTION isFork is not the used name by REST API.
                (Boolean) item.get("archived"),
                (Boolean) item.get("disabled"),
                licenceName
        );
    }

    /**
     * Create an empty PageInfo for cases where no results are returned
     */
    private static PageInfoDto createDummyEmptyPageInfo() {
        return new PageInfoDto(false, false, null, null);
    }

    private static Instant parseInstant(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp: {}", timestamp, e);
            return null;
        }
    }
}
