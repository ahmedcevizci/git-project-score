package tech.alaz.git.project.score.github.client.graphql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GitHubGraphQlClientConfig {

    @Value("${git-project-score.github.api.token:}")
    private String githubToken;

    @Bean
    public HttpGraphQlClient gitHubGraphQLClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.github.com/graphql")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        // Add authorization header if token is not provided
        if (githubToken == null || githubToken.isEmpty()) {
            throw new Error("GitHub API token is not provided! The application cannot function without a valid token. " +
                    "\nPlease provide a GitHub API token by setting `git-project-score.github.api.token` in `application.properties`.");
        }

        builder.defaultHeader("Authorization", "Bearer " + githubToken);
        WebClient webClient = builder.build();
        return HttpGraphQlClient.builder(webClient).build();
    }
}
