package tech.alaz.git.project.score.domain.score;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ScoreOverHundredTest {

    @Test
    void shouldCreateValidScore() {
        ScoreOverHundred score = new ScoreOverHundred(50);
        assertEquals(50, score.getScore());
    }

    @Test
    void shouldCreateScoreAtMinimumBound() {
        ScoreOverHundred score = new ScoreOverHundred(0);
        assertEquals(0, score.getScore());
    }

    @Test
    void shouldCreateScoreAtMaximumBound() {
        ScoreOverHundred score = new ScoreOverHundred(100);
        assertEquals(100, score.getScore());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10, -100, 101, 150, 200})
    void shouldThrowExceptionForInvalidScore(int invalidScore) {
        assertThrows(IllegalArgumentException.class, () -> new ScoreOverHundred(invalidScore));
    }

    @Test
    void shouldReturnMaxScore() {
        ScoreOverHundred score = new ScoreOverHundred(50);
        assertEquals(100, score.maxScore());
    }

    @Test
    void shouldReturnMinScore() {
        ScoreOverHundred score = new ScoreOverHundred(50);
        assertEquals(0, score.minScore());
    }

    @Test
    void shouldConvertToOverFive() {
        assertEquals(0, new ScoreOverHundred(0).toOverFive());
        assertEquals(1, new ScoreOverHundred(10).toOverFive());  // 0.5 rounds to 1
        assertEquals(1, new ScoreOverHundred(15).toOverFive());  // 0.75 rounds to 1
        assertEquals(1, new ScoreOverHundred(20).toOverFive());  // 1.0
        assertEquals(3, new ScoreOverHundred(50).toOverFive());  // 2.5 rounds to 3
        assertEquals(4, new ScoreOverHundred(75).toOverFive());  // 3.75 rounds to 4
        assertEquals(5, new ScoreOverHundred(100).toOverFive()); // 5.0
    }

    @Test
    void shouldConvertToOverTen() {
        assertEquals(0, new ScoreOverHundred(0).toOverTen());
        assertEquals(1, new ScoreOverHundred(10).toOverTen());
        assertEquals(2, new ScoreOverHundred(20).toOverTen());
        assertEquals(5, new ScoreOverHundred(50).toOverTen());
        assertEquals(8, new ScoreOverHundred(75).toOverTen());
        assertEquals(10, new ScoreOverHundred(100).toOverTen());
    }

    @Test
    void shouldRoundCorrectlyWhenConvertingScales() {
        // Test rounding behavior
        assertEquals(1, new ScoreOverHundred(24).toOverFive()); // 1.2 rounds to 1
        assertEquals(1, new ScoreOverHundred(26).toOverFive()); // 1.3 rounds to 1
    }

    @Test
    void shouldBeEqualWhenScoresAreEqual() {
        ScoreOverHundred score1 = new ScoreOverHundred(75);
        ScoreOverHundred score2 = new ScoreOverHundred(75);
        assertEquals(score1, score2);
        assertEquals(score1.hashCode(), score2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenScoresAreDifferent() {
        ScoreOverHundred score1 = new ScoreOverHundred(75);
        ScoreOverHundred score2 = new ScoreOverHundred(80);
        assertNotEquals(score1, score2);
    }

    @Test
    void shouldNotBeEqualToNull() {
        ScoreOverHundred score = new ScoreOverHundred(75);
        assertNotEquals(null, score);
    }

    @Test
    void shouldNotBeEqualToDifferentClass() {
        ScoreOverHundred score = new ScoreOverHundred(75);
        assertNotEquals(score, "75");
    }

    @Test
    void shouldHaveCorrectToStringFormat() {
        ScoreOverHundred score = new ScoreOverHundred(75);
        assertEquals("ScoreOverHundred{75/100}", score.toString());
    }
}
