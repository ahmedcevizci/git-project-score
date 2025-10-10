package tech.alaz.git.project.score.api.controller.exception;

public class PageSizeCannotExceedMaxValueException extends RuntimeException {

    private final int maxPageSize;

    public PageSizeCannotExceedMaxValueException(int maxPageSize, String message) {
        super(message);
        this.maxPageSize = maxPageSize;
    }

    public PageSizeCannotExceedMaxValueException(int maxPageSize, String message, Throwable cause) {
        super(message, cause);
        this.maxPageSize = maxPageSize;
    }
}