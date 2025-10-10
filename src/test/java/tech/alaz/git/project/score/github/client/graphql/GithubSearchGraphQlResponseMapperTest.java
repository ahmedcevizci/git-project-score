package tech.alaz.git.project.score.github.client.graphql;

import org.junit.jupiter.api.Test;
import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.github.client.graphql.dto.GitHubGraphQlSearchResponseDto;
import tech.alaz.git.project.score.github.client.graphql.dto.PageInfoDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GithubSearchGraphQlResponseMapperTest {

    @Test
    void shouldReturnEmptyResponseForNullInput() {
        GitHubGraphQlSearchResponseDto result = GithubSearchGraphQlResponseMapper.parseResponse(null);

        assertNotNull(result);
        assertEquals(0, result.totalResultCount());
        assertNotNull(result.pageInfo());
        assertFalse(result.pageInfo().hasNextPage());
        assertFalse(result.pageInfo().hasPreviousPage());
        assertNull(result.pageInfo().startCursor());
        assertNull(result.pageInfo().endCursor());
        assertTrue(result.repositories().isEmpty());
    }

    @Test
    void shouldReturnEmptyResponseForEmptyEdges() {
        Map<String, Object> response = Map.of(
                "repositoryCount", 0,
                "edges", List.of(),
                "pageInfo", Map.of(
                        "hasNextPage", false,
                        "hasPreviousPage", false
                )
        );

        GitHubGraphQlSearchResponseDto result = GithubSearchGraphQlResponseMapper.parseResponse(response);

        assertEquals(0, result.totalResultCount());
        assertTrue(result.repositories().isEmpty());
    }

    @Test
    void shouldParsePageInfoCorrectly() {
        Map<String, Object> response = Map.of(
                "repositoryCount", 100,
                "pageInfo", Map.of(
                        "hasNextPage", true,
                        "hasPreviousPage", false,
                        "startCursor", "cursor1",
                        "endCursor", "cursor2"
                )
        );

        GitHubGraphQlSearchResponseDto result = GithubSearchGraphQlResponseMapper.parseResponse(response);

        assertEquals(100, result.totalResultCount());
        assertTrue(result.pageInfo().hasNextPage());
        assertFalse(result.pageInfo().hasPreviousPage());
        assertEquals("cursor1", result.pageInfo().startCursor());
        assertEquals("cursor2", result.pageInfo().endCursor());
    }

    @Test
    void shouldParseCompleteRepositoryNode() {
        Map<String, Object> node = Map.ofEntries(
                Map.entry("name", "test-repo"),
                Map.entry("description", "A test repository"),
                Map.entry("url", "https://github.com/test/test-repo"),
                Map.entry("primaryLanguage", Map.of("name", "Java")),
                Map.entry("createdAt", "2020-01-01T00:00:00Z"),
                Map.entry("updatedAt", "2024-01-01T00:00:00Z"),
                Map.entry("pushedAt", "2024-01-01T00:00:00Z"),
                Map.entry("stargazerCount", 100),
                Map.entry("watchers", Map.of("totalCount", 50)),
                Map.entry("forkCount", 25),
                Map.entry("isFork", false),
                Map.entry("isArchived", false),
                Map.entry("isDisabled", false),
                Map.entry("licenseInfo", Map.of("name", "MIT License"))
        );

        Map<String, Object> edge = Map.of("node", node);

        GithubRepositoryDto result = GithubSearchGraphQlResponseMapper.mapToGithubRepositoryDto(edge);

        assertEquals("test-repo", result.name());
        assertEquals("A test repository", result.description());
        assertEquals("https://github.com/test/test-repo", result.htmlUrl());
        assertEquals("Java", result.primaryLanguage());
        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), result.createdAt());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), result.updatedAt());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), result.pushedAt());
        assertEquals(100, result.stargazerCount());
        assertEquals(50, result.watcherCount());
        assertEquals(25, result.forkCount());
        assertFalse(result.isFork());
        assertFalse(result.isArchived());
        assertFalse(result.isDisabled());
        assertEquals("MIT License", result.licenseName());
    }

    @Test
    void shouldHandleMissingOptionalFields() {
        Map<String, Object> edge = Map.of(
                "node", Map.of(
                        "name", "minimal-repo",
                        "url", "https://github.com/test/minimal-repo",
                        "stargazerCount", 0,
                        "forkCount", 0,
                        "isFork", false,
                        "isArchived", false,
                        "isDisabled", false
                )
        );

        GithubRepositoryDto result = GithubSearchGraphQlResponseMapper.mapToGithubRepositoryDto(edge);

        assertEquals("minimal-repo", result.name());
        assertNull(result.description());
        assertNull(result.primaryLanguage());
        assertNull(result.createdAt());
        assertNull(result.updatedAt());
        assertNull(result.pushedAt());
        assertNull(result.watcherCount());
        assertNull(result.licenseName());
    }

    @Test
    void shouldCreateEmptyPageInfo() {
        PageInfoDto pageInfo = GithubSearchGraphQlResponseMapper.createEmptyPageInfo();

        assertNotNull(pageInfo);
        assertFalse(pageInfo.hasNextPage());
        assertFalse(pageInfo.hasPreviousPage());
        assertNull(pageInfo.startCursor());
        assertNull(pageInfo.endCursor());
    }
}
