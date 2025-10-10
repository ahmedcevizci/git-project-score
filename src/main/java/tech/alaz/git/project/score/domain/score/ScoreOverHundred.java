package tech.alaz.git.project.score.domain.score;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreOverHundred {

    private static final Logger logger = LoggerFactory.getLogger(ScoreOverHundred.class);

    @Min(0)
    @Max(100)
    private int score;

    public ScoreOverHundred(int score) {
        if (score < 0 || score > 100) {
            logger.error("Invalid score value: {}. Score must be between 0 and 100", score);
            //TODO: create custom exception
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
        this.score = score;
    }

    public static ScoreOverHundred score(int score) {
        return new ScoreOverHundred(score);
    }

    public int maxScore() {
        return 100;
    }

    public int minScore() {
        return 0;
    }

    public int getScore() {
        return this.score;
    }

    /**
     * Converts the score to a scale over 5
     *
     * @return score converted to range [0, 5]
     */
    public int toOverFive() {
        return convertToScale(5);
    }

    /**
     * Converts the score to a scale over 10
     *
     * @return score converted to range [0, 10]
     */
    public int toOverTen() {
        return convertToScale(10);
    }

    /**
     * Helper method to convert score to any target scale
     *
     * @param targetMax the maximum value of the target scale
     * @return score converted to range [0, targetMax]
     */
    private int convertToScale(int targetMax) {
        double normalized = (double) (this.score) / 100;
        int converted = (int) Math.round(normalized * targetMax);

        // Ensure the result is within bounds by avoiding overflows
        return Math.min(targetMax, converted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoreOverHundred that = (ScoreOverHundred) o;
        return score == that.score;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(score);
    }

    @Override
    public String toString() {
        return "ScoreOverHundred{" + score + "/100}";
    }
}
