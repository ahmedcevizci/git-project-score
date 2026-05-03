package tech.alaz.git.project.score.github.client.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
public class GitHubRestClientConfig {

    @Value("${git-project-score.github.api.token:}")
    private String githubToken;

    public static final String STARS = "stars";
    public static final String FORKS = "forks";

    @Bean
    public RestClient gitHubRestClient() {
        if (githubToken == null || githubToken.isEmpty()) {
            throw new Error("GitHub API token is not provided! The application cannot function without a valid token. " +
                    "\nPlease provide a GitHub API token by setting `git-project-score.github.api.token` in `application.properties`.");
        }
        return RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("Authorization", "token " + githubToken)
                .build();
    }
}