package tech.alaz.git.project.score.api.controller.exception;

public class CreationDateCannotBeInFutureException extends RuntimeException {

    public CreationDateCannotBeInFutureException(String message) {
        super(message);
    }

    public CreationDateCannotBeInFutureException(String message, Throwable cause) {
        super(message, cause);
    }
}