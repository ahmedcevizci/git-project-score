package tech.alaz.git.project.score.github.client.rest;

import org.junit.jupiter.api.Test;
import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.github.client.graphql.dto.GitHubGraphQlSearchResponseDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GitHubSearchRestResponseMapperTest {

    @Test
    void shouldReturnEmptyResponseForNullInput() {
        GitHubGraphQlSearchResponseDto result = GitHubSearchRestResponseMapper.parseRestApiResponse(null);

        assertNotNull(result);
        assertEquals(0, result.totalResultCount());
        assertNotNull(result.pageInfo());
        assertFalse(result.pageInfo().hasNextPage());
        assertFalse(result.pageInfo().hasPreviousPage());
        assertTrue(result.repositories().isEmpty());
    }

    @Test
    void shouldReturnEmptyResponseForNullItems() {
        Map<String, Object> response = Map.of(
                "total_count", 0,
                "incomplete_results", false
        );

        GitHubGraphQlSearchResponseDto result = GitHubSearchRestResponseMapper.parseRestApiResponse(response);

        assertEquals(0, result.totalResultCount());
        assertTrue(result.repositories().isEmpty());
    }

    @Test
    void shouldHandleEmptyItems() {
        Map<String, Object> response = Map.of(
                "total_count", 0,
                "incomplete_results", false,
                "items", List.of()
        );

        GitHubGraphQlSearchResponseDto result = GitHubSearchRestResponseMapper.parseRestApiResponse(response);

        assertEquals(0, result.totalResultCount());
        assertTrue(result.repositories().isEmpty());
    }

    @Test
    void shouldParseCompleteRestApiItem() {
        Map<String, Object> item = Map.ofEntries(
                Map.entry("name", "test-repo"),
                Map.entry("description", "A test repository"),
                Map.entry("html_url", "https://github.com/test/test-repo"),
                Map.entry("language", "Java"),
                Map.entry("created_at", "2020-01-01T00:00:00Z"),
                Map.entry("updated_at", "2024-01-01T00:00:00Z"),
                Map.entry("pushed_at", "2024-01-01T00:00:00Z"),
                Map.entry("stargazers_count", 100),
                Map.entry("watchers_count", 50),
                Map.entry("forks_count", 25),
                Map.entry("fork", false),
                Map.entry("archived", false),
                Map.entry("disabled", false),
                Map.entry("license", Map.of("name", "MIT License"))
        );

        Map<String, Object> response = Map.of(
                "total_count", 1,
                "incomplete_results", false,
                "items", List.of(item)
        );

        GitHubGraphQlSearchResponseDto result = GitHubSearchRestResponseMapper.parseRestApiResponse(response);

        assertEquals(1, result.totalResultCount());
        assertEquals(1, result.repositories().size());

        GithubRepositoryDto repo = result.repositories().get(0);
        assertEquals("test-repo", repo.name());
        assertEquals("A test repository", repo.description());
        assertEquals("https://github.com/test/test-repo", repo.htmlUrl());
        assertEquals("Java", repo.primaryLanguage());
        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), repo.createdAt());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), repo.updatedAt());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), repo.pushedAt());
        assertEquals(100, repo.stargazerCount());
        assertEquals(50, repo.watcherCount());
        assertEquals(25, repo.forkCount());
        assertFalse(repo.isFork());
        assertFalse(repo.isArchived());
        assertFalse(repo.isDisabled());
        assertEquals("MIT License", repo.licenseName());
    }

    @Test
    void shouldHandleMissingOptionalFields() {
        Map<String, Object> item = Map.of(
                "name", "minimal-repo",
                "html_url", "https://github.com/test/minimal-repo",
                "stargazers_count", 0,
                "watchers_count", 0,
                "forks_count", 0,
                "fork", false,
                "archived", false,
                "disabled", false
        );

        Map<String, Object> response = Map.of(
                "total_count", 1,
                "items", List.of(item)
        );

        GitHubGraphQlSearchResponseDto result = GitHubSearchRestResponseMapper.parseRestApiResponse(response);

        assertEquals(1, result.repositories().size());
        GithubRepositoryDto repo = result.repositories().get(0);

        assertEquals("minimal-repo", repo.name());
        assertNull(repo.description());
        assertNull(repo.primaryLanguage());
        assertNull(repo.createdAt());
        assertNull(repo.updatedAt());
        assertNull(repo.pushedAt());
        assertNull(repo.licenseName());
    }

    @Test
    void shouldHandleMultipleItems() {
        Map<String, Object> item1 = Map.of(
                "name", "repo1",
                "html_url", "https://github.com/test/repo1",
                "stargazers_count", 10,
                "watchers_count", 5,
                "forks_count", 2,
                "fork", false,
                "archived", false,
                "disabled", false
        );

        Map<String, Object> item2 = Map.of(
                "name", "repo2",
                "html_url", "https://github.com/test/repo2",
                "stargazers_count", 20,
                "watchers_count", 10,
                "forks_count", 5,
                "fork", true,
                "archived", true,
                "disabled", false
        );

        Map<String, Object> response = Map.of(
                "total_count", 2,
                "items", List.of(item1, item2)
        );

        GitHubGraphQlSearchResponseDto result = GitHubSearchRestResponseMapper.parseRestApiResponse(response);

        assertEquals(2, result.totalResultCount());
        assertEquals(2, result.repositories().size());
        assertEquals("repo1", result.repositories().get(0).name());
        assertEquals("repo2", result.repositories().get(1).name());
        assertFalse(result.repositories().get(0).isFork());
        assertTrue(result.repositories().get(1).isFork());
    }
}
