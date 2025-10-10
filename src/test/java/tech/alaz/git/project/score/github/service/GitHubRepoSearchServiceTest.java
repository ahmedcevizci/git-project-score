package tech.alaz.git.project.score.github.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tech.alaz.git.project.score.api.controller.dto.ScoredGitHubRepoSearchResponseDto;
import tech.alaz.git.project.score.domain.score.strategy.DefaultScoringStrategy;
import tech.alaz.git.project.score.domain.score.strategy.ScoringStrategy;
import tech.alaz.git.project.score.github.client.graphql.dto.GitHubGraphQlSearchResponseDto;
import tech.alaz.git.project.score.github.client.graphql.exception.GitHubGraphQlClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitHubRepoSearchServiceTest {

    @Mock
    private HttpGraphQlClient gitHubGraphQlClient;

    @Mock
    private WebClient gitHubRestWebClient;

    @Mock
    private HttpGraphQlClient.RequestSpec requestSpec;

    @Mock
    private HttpGraphQlClient.RetrieveSpec retrieveSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GitHubRepoSearchService service;
    private final int maxPageSize = 5;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GitHubRepoSearchService(gitHubGraphQlClient, gitHubRestWebClient, maxPageSize);
    }

    @Test
    void shouldBuildSearchQueryWithAllParameters() {
        String searchTerm = "spring";
        String language = "Java";
        LocalDate createdAfter = LocalDate.of(2020, 1, 1);

        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(Mono.just(Map.of()));

        service.searchGitHubReposViaGraphQl(searchTerm, language, createdAfter, 10, null);

        verify(gitHubGraphQlClient).document(anyString());
        verify(requestSpec, times(3)).variable(anyString(), any());
    }

    @Test
    void shouldThrowExceptionWhenNoSearchParametersProvided() {
        Mono<GitHubGraphQlSearchResponseDto> result =
                service.searchGitHubReposViaGraphQl(null, null, null, 10, null);

        assertThrows(GitHubGraphQlClientException.class, result::block);
    }

    @Test
    void shouldBuildSearchQueryWithOnlyLanguageAndCreatedAfterFilter() {
        String language = "Java";
        LocalDate createdAfter = LocalDate.of(2020, 1, 1);

        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(Mono.just(Map.of(
                "repositoryCount", 0,
                "edges", List.of(),
                "pageInfo", Map.of("hasNextPage", false, "hasPreviousPage", false)
        )));

        Mono<GitHubGraphQlSearchResponseDto> result =
                service.searchGitHubReposViaGraphQl(null, language, createdAfter, 10, null);

        assertNotNull(result);
        verify(gitHubGraphQlClient).document(anyString());
    }

    @Test
    void shouldHandleSearchWithCreatedAfterFilter() {
        LocalDate createdAfter = LocalDate.of(2020, 1, 1);

        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(Mono.just(Map.of(
                "repositoryCount", 0,
                "edges", List.of(),
                "pageInfo", Map.of("hasNextPage", false, "hasPreviousPage", false)
        )));

        Mono<GitHubGraphQlSearchResponseDto> result =
                service.searchGitHubReposViaGraphQl(null, "Java", createdAfter, 10, null);

        assertNotNull(result);
        verify(requestSpec).variable(eq("query"), argThat(query ->
                query.toString().contains("created:>=2020-01-01")
        ));
    }

    @Test
    void shouldSearchAndScoreGitHubReposSuccessfully() {
        // Setup test data
        LocalDate createdAfter = LocalDate.of(2020, 1, 1);
        Instant now = Instant.now();

        Map<String, Object> mockRestResponse = Map.of(
                "total_count", 1,
                "items", List.of(Map.of(
                        "name", "max-repo",
                        "html_url", "url",
                        "stargazers_count", 100,
                        "watchers_count", 50,
                        "forks_count", 100,
                        "fork", false,
                        "archived", false,
                        "disabled", false
                ))
        );

        // Setup mocks for GraphQL
        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(Mono.just(Map.of(
                "repositoryCount", 1,
                "edges", List.of(Map.of("node", Map.of(
                        "name", "test-repo",
                        "url", "url",
                        "stargazerCount", 50,
                        "forkCount", 25,
                        "updatedAt", now.toString(),
                        "isFork", false,
                        "isArchived", false,
                        "isDisabled", false
                ))),
                "pageInfo", Map.of("hasNextPage", false, "hasPreviousPage", false)
        )));

        // Setup mocks for REST API (max values)
        when(gitHubRestWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockRestResponse));

        ScoringStrategy strategy = new DefaultScoringStrategy();

        // Execute
        Mono<ScoredGitHubRepoSearchResponseDto> result =
                service.searchAndScoreGitHubRepos("test", "Java", createdAfter, 10, null, strategy);

        // Verify
        ScoredGitHubRepoSearchResponseDto response = result.block();
        assertNotNull(response);
        assertEquals(1, response.totalResultCount());
        assertEquals(100, response.maxStarGazersCount());
        assertEquals(100, response.maxForkCount());
        assertFalse(response.githubRepositories().isEmpty());
    }

    @Test
    void shouldHandleEmptySearchResults() {
        LocalDate createdAfter = LocalDate.of(2020, 1, 1);

        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(Mono.just(Map.of(
                "repositoryCount", 0,
                "edges", List.of(),
                "pageInfo", Map.of("hasNextPage", false, "hasPreviousPage", false)
        )));

        when(gitHubRestWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of(
                "total_count", 0,
                "items", List.of()
        )));

        ScoringStrategy strategy = new DefaultScoringStrategy();

        Mono<ScoredGitHubRepoSearchResponseDto> result =
                service.searchAndScoreGitHubRepos(null, "Java", createdAfter, 10, null, strategy);

        ScoredGitHubRepoSearchResponseDto response = result.block();
        assertNotNull(response);
        assertEquals(0, response.totalResultCount());
        assertTrue(response.githubRepositories().isEmpty());
    }


    @Test
    void shouldUseMaxPageSizeWhenFirstIsNull() {
        LocalDate createdAfter = LocalDate.of(2020, 1, 1);

        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(eq("first"), eq(maxPageSize))).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(Mono.just(Map.of(
                "repositoryCount", 0,
                "edges", List.of(),
                "pageInfo", Map.of("hasNextPage", false, "hasPreviousPage", false)
        )));

        service.searchGitHubReposViaGraphQl(null, "Java", createdAfter, null, null);

        verify(requestSpec).variable(eq("first"), eq(maxPageSize));
    }

    @Test
    void shouldPropagateAfterCursorForPagination() {
        LocalDate createdAfter = LocalDate.of(2020, 1, 1);
        String cursor = "Y3Vyc29yOnYyOpK5MjAyMC0wMS0wMVQwMDowMDowMFo=";

        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.variable(eq("after"), eq(cursor))).thenReturn(requestSpec);
        when(requestSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(Mono.just(Map.of(
                "repositoryCount", 0,
                "edges", List.of(),
                "pageInfo", Map.of("hasNextPage", false, "hasPreviousPage", false)
        )));

        service.searchGitHubReposViaGraphQl(null, "Java", createdAfter, 10, cursor);

        verify(requestSpec).variable(eq("after"), eq(cursor));
    }
}
