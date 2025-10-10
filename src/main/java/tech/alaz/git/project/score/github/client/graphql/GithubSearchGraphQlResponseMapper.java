package tech.alaz.git.project.score.github.client.graphql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.github.client.graphql.dto.GitHubGraphQlSearchResponseDto;
import tech.alaz.git.project.score.github.client.graphql.dto.PageInfoDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GithubSearchGraphQlResponseMapper {

    private static final Logger logger = LoggerFactory.getLogger(GithubSearchGraphQlResponseMapper.class);

    @SuppressWarnings("unchecked")
    public static GitHubGraphQlSearchResponseDto parseResponse(Map<String, Object> response) {
        logger.debug("Parsing GraphQL response");

        if (response == null) {
            logger.warn("Received null response from GraphQL API");
            return new GitHubGraphQlSearchResponseDto(0, createEmptyPageInfo(), List.of());
        }

        Integer repositoryCount = (Integer) response.get("repositoryCount");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) response.get("edges");
        Map<String, Object> pageInfoMap = (Map<String, Object>) response.get("pageInfo");

        logger.debug("GraphQL response: repositoryCount={}, edges={}",
                repositoryCount, edges != null ? edges.size() : 0);

        PageInfoDto pageInfo = parsePageInfo(pageInfoMap);

        if (edges == null || edges.isEmpty()) {
            logger.warn("No edges found in GraphQL response");
            return new GitHubGraphQlSearchResponseDto(
                    repositoryCount != null ? repositoryCount : 0,
                    pageInfo,
                    List.of()
            );
        }

        List<GithubRepositoryDto> repositories = edges
                .stream()
                .filter(Objects::nonNull)
                .map(GithubSearchGraphQlResponseMapper::mapToGithubRepositoryDto)
                .toList();

        logger.debug("Successfully parsed {} repositories from GraphQL response", repositories.size());
        return new GitHubGraphQlSearchResponseDto(
                repositoryCount != null ? repositoryCount : 0,
                pageInfo,
                repositories
        );
    }

    private static PageInfoDto parsePageInfo(Map<String, Object> pageInfoMap) {
        if (pageInfoMap == null) {
            logger.debug("No pageInfo in response, using empty page info");
            return createEmptyPageInfo();
        }

        PageInfoDto pageInfo = new PageInfoDto(
                (Boolean) pageInfoMap.getOrDefault("hasNextPage", false),
                (Boolean) pageInfoMap.getOrDefault("hasPreviousPage", false),
                (String) pageInfoMap.get("startCursor"),
                (String) pageInfoMap.get("endCursor")
        );

        logger.debug("Parsed pageInfo: hasNextPage={}, hasPreviousPage={}",
                pageInfo.hasNextPage(), pageInfo.hasPreviousPage());
        return pageInfo;
    }

    /**
     * Create an empty PageInfo for cases where no results are returned
     */
    public static PageInfoDto createEmptyPageInfo() {
        return new PageInfoDto(false, false, null, null);
    }


    @SuppressWarnings("unchecked")
    public static GithubRepositoryDto mapToGithubRepositoryDto(Map<String, Object> edge) {
        Map<String, Object> node = (Map<String, Object>) edge.get("node");
        String repoName = (String) node.get("name");
        logger.debug("Mapping GraphQL node for repository: {}", repoName);

        // Parse primary language
        String primaryLanguage = null;
        Map<String, Object> primaryLanguageMap = (Map<String, Object>) node.get("primaryLanguage");
        if (primaryLanguageMap != null) {
            primaryLanguage = (String) primaryLanguageMap.get("name");
        }

        // Parse license info
        Map<String, Object> licenseInfo = (Map<String, Object>) node.get("licenseInfo");
        String licenseName = null;
        if (licenseInfo != null) {
            licenseName = (String) licenseInfo.get("name");
        }

        // Parse watcher count
        Integer watcherCount = null;
        Map<String, Object> watchers = (Map<String, Object>) node.get("watchers");
        if (watchers != null) {
            watcherCount = (Integer) watchers.get("totalCount");
        }

        return new GithubRepositoryDto(
                repoName,
                (String) node.get("description"),
                (String) node.get("url"),
                primaryLanguage,
                parseInstant((String) node.get("createdAt")),
                parseInstant((String) node.get("updatedAt")),
                parseInstant((String) node.get("pushedAt")),
                (Integer) node.get("stargazerCount"),
                watcherCount,
                (Integer) node.get("forkCount"),
                (Boolean) node.get("isFork"),
                (Boolean) node.get("isArchived"),
                (Boolean) node.get("isDisabled"),
                licenseName
        );
    }

    private static Instant parseInstant(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            logger.error("Failed to parse timestamp: {}", timestamp, e);
            return null;
        }
    }
}
