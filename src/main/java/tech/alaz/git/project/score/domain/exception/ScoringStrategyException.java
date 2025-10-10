package tech.alaz.git.project.score.domain.exception;

public class ScoringStrategyException extends RuntimeException {

    public ScoringStrategyException(String message) {
        super(message);
    }

    public ScoringStrategyException(String message, Throwable cause) {
        super(message, cause);
    }
}
