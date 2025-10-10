package tech.alaz.git.project.score.github.client.graphql.exception;

public class GitHubGraphQlClientException extends RuntimeException {

    public GitHubGraphQlClientException(String message) {
        super(message);
    }

    public GitHubGraphQlClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
