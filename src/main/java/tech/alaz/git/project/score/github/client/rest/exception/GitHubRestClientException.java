package tech.alaz.git.project.score.github.client.rest.exception;

public class GitHubRestClientException extends RuntimeException {

    public GitHubRestClientException(String message) {
        super(message);
    }

    public GitHubRestClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
