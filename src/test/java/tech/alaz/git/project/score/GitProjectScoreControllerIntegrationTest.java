package tech.alaz.git.project.score;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "GITHUB_API_TOKEN=dummy-test-token")
@AutoConfigureMockMvc
class GitProjectScoreControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HttpSyncGraphQlClient gitHubGraphQlClient;

    @MockitoBean
    private RestClient gitHubRestWebClient;

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnSuccessfullyWithValidParameters() throws Exception {
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

        GraphQlClient.RequestSpec requestSpec = mock(GraphQlClient.RequestSpec.class);
        GraphQlClient.RetrieveSyncSpec retrieveSpec = mock(GraphQlClient.RetrieveSyncSpec.class);
        @SuppressWarnings("rawtypes") RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        @SuppressWarnings("rawtypes") RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(gitHubGraphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variable(anyString(), any())).thenReturn(requestSpec);
        when(requestSpec.retrieveSync(anyString())).thenReturn(retrieveSpec);
        when(retrieveSpec.toEntity(Map.class)).thenReturn(mockGraphQLResponse);

        when(gitHubRestWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(mockRestResponse);

        mockMvc.perform(get("/api/search")
                        .param("language", "Java")
                        .param("creationDate", "01-01-2020")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResultCount").exists())
                .andExpect(jsonPath("$.maxStarGazersCount").exists())
                .andExpect(jsonPath("$.maxForkCount").exists());
    }

    @Test
    void shouldReturnBadRequestWhenPageSizeExceedsMax() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("language", "Java")
                        .param("creationDate", "01-01-2020")
                        .param("pageSize", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnBadRequestWhenCreationDateIsInFuture() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("language", "Java")
                        .param("creationDate", "01-01-2030")
                        .param("pageSize", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnBadRequestWhenInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("language", "Java")
                        .param("creationDate", "invalid-date")
                        .param("pageSize", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRequiredParametersMissing() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest());
    }
}