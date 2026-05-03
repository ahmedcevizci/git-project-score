package tech.alaz.git.project.score;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that incoming HTTP requests are dispatched on virtual threads by Tomcat.
 * Uses RANDOM_PORT so requests go through Tomcat's actual thread pool,
 * unlike MockMvc which bypasses it.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "GITHUB_API_TOKEN=dummy-test-token"
)
class VirtualThreadVerificationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private HttpSyncGraphQlClient gitHubGraphQlClient;

    @MockitoBean
    private RestClient gitHubRestWebClient;

    @Autowired
    private ThreadCapture threadCapture;

    @Test
    void tomcatShouldDispatchRequestsOnVirtualThreads() throws Exception {
        HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/actuator/health"))
                        .GET().build(),
                HttpResponse.BodyHandlers.discarding());

        assertThat(threadCapture.isVirtual.get())
                .as("Tomcat request thread should be a virtual thread")
                .isTrue();
        assertThat(threadCapture.threadName.get())
                .as("Virtual thread name should carry Tomcat's virtual thread prefix")
                .startsWith("tomcat-handler-");
    }

    @TestConfiguration
    static class Config {

        private final ThreadCapture capture = new ThreadCapture();

        @Bean
        ThreadCapture threadCapture() {
            return capture;
        }

        @Bean
        Filter threadCaptureFilter() {
            return (request, response, chain) -> {
                Thread t = Thread.currentThread();
                capture.isVirtual.set(t.isVirtual());
                capture.threadName.set(t.getName());
                chain.doFilter(request, response);
            };
        }
    }

    static class ThreadCapture {
        final AtomicBoolean isVirtual = new AtomicBoolean();
        final AtomicReference<String> threadName = new AtomicReference<>();
    }
}