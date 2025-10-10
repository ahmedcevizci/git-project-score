package tech.alaz.git.project.score;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GitProjectScoreControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private HttpGraphQlClient gitHubGraphQlClient;

    @MockitoBean
    private WebClient gitHubRestWebClient;

    @MockitoBean
    private HttpGraphQlClient.RequestSpec requestSpec;

    @MockitoBean
    private HttpGraphQlClient.RetrieveSpec retrieveSpec;

    @MockitoBean
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @MockitoBean
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @MockitoBean
    private WebClient.ResponseSpec responseSpec;

    @Test
    void shouldReturnSuccessfullyWithValidParameters() {
        Map<String, Object> mockGraphQLResponse = Map.of(
                "repositoryCount", 0,
                "edges", List.of(),
                "pageInfo", Map.of("hasNextPage", false, "hasPreviousPage", false)
        );

        Map<String, Object> mockRestResponse = Map.of(
                "total_count", 0,
                "incomplete_results", false,
                "items", List.of()
        );

        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.retrieve(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(Mono.just(mockGraphQLResponse));

        when(gitHubRestWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockRestResponse));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/search")
                        .queryParam("language", "Java")
                        .queryParam("creationDate", "01-01-2020")
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalResultCount").exists()
                .jsonPath("$.maxStarGazersCount").exists()
                .jsonPath("$.maxForkCount").exists();
    }

    @Test
    void shouldReturnBadRequestWhenPageSizeExceedsMax() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/search")
                        .queryParam("language", "Java")
                        .queryParam("creationDate", "01-01-2020")
                        .queryParam("pageSize", 100)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").exists();
    }

    @Test
    void shouldReturnBadRequestWhenCreationDateIsInFuture() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/search")
                        .queryParam("language", "Java")
                        .queryParam("creationDate", "01-01-2030")
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").exists();
    }

    @Test
    void shouldReturnBadRequestWhenInvalidDateFormat() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/search")
                        .queryParam("language", "Java")
                        .queryParam("creationDate", "invalid-date")
                        .queryParam("pageSize", 3)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnBadRequestWhenRequiredParametersMissing() {
        webTestClient.get()
                .uri("/api/search")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
