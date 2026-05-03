package tech.alaz.git.project.score.github.client.graphql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

@Configuration
public class GitHubGraphQlClientConfig {

    @Value("${git-project-score.github.api.token:}")
    private String githubToken;

    @Bean
    public HttpSyncGraphQlClient gitHubGraphQLClient() {
        if (githubToken == null || githubToken.isEmpty()) {
            throw new Error("GitHub API token is not provided! The application cannot function without a valid token. " +
                    "\nPlease provide a GitHub API token by setting `git-project-score.github.api.token` in `application.properties`.");
        }
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.github.com/graphql")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .build();
        return HttpSyncGraphQlClient.builder(restClient).build();
    }
}