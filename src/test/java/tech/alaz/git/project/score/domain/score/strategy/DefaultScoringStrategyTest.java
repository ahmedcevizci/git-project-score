package tech.alaz.git.project.score.domain.score.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.domain.dto.ScoreDto;
import tech.alaz.git.project.score.domain.exception.ScoringStrategyException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class DefaultScoringStrategyTest {

    private DefaultScoringStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DefaultScoringStrategy();
    }

    @Test
    void shouldReturnTrueForIsRelativeToOtherProjects() {
        assertTrue(strategy.isRelativeToOtherProjects());
    }

    // Star Score Tests
    @Test
    void shouldCalculateStarScoreLinearlyForZeroStars() {
        assertEquals(0, strategy.calculateStarSuccessScoreLinear(0, 100));
    }

    @Test
    void shouldCalculateStarScoreLinearlyForMaxStars() {
        assertEquals(100, strategy.calculateStarSuccessScoreLinear(100, 100));
    }

    @Test
    void shouldCalculateStarScoreLinearlyForHalfMaxStars() {
        assertEquals(50, strategy.calculateStarSuccessScoreLinear(50, 100));
    }

    @Test
    void shouldCalculateStarScoreLinearlyForQuarterMaxStars() {
        assertEquals(25, strategy.calculateStarSuccessScoreLinear(25, 100));
    }

    @Test
    void shouldReturnIllegalStateExceptionWhenStarsExceedMax() {
        assertThrows(IllegalStateException.class,
                () -> strategy.calculateStarSuccessScoreLinear(150, 100));
    }

    @Test
    void shouldThrowExceptionForNegativeMaxStarCount() {
        assertThrows(ScoringStrategyException.class,
                () -> strategy.calculateStarSuccessScoreLinear(-2, -1));
    }

    @Test
    void shouldThrowExceptionForNegativeRepoStarCount() {
        assertThrows(ScoringStrategyException.class,
                () -> strategy.calculateStarSuccessScoreLinear(-1, 1));
    }

    // Fork Score Tests
    @Test
    void shouldCalculateForkScoreLinearlyForZeroForks() {
        assertEquals(0, strategy.calculateForkSuccessScoreLinear(0, 100));
    }

    @Test
    void shouldCalculateForkScoreLinearlyForMaxForks() {
        assertEquals(100, strategy.calculateForkSuccessScoreLinear(100, 100));
    }

    @Test
    void shouldCalculateForkScoreLinearlyForHalfMaxForks() {
        assertEquals(50, strategy.calculateForkSuccessScoreLinear(50, 100));
    }

    @Test
    void shouldReturnIllegalStateExceptionWhenForksExceedMax() {
        assertThrows(IllegalStateException.class,
                () -> strategy.calculateForkSuccessScoreLinear(150, 100));
    }

    @Test
    void shouldThrowExceptionForNegativeMaxForkCount() {
        assertThrows(ScoringStrategyException.class,
                () -> strategy.calculateForkSuccessScoreLinear(-2, -1));
    }

    @Test
    void shouldThrowExceptionForNegativeRepoForkCount() {
        assertThrows(ScoringStrategyException.class,
                () -> strategy.calculateForkSuccessScoreLinear(-1, 1));
    }

    // Date Recency Score Tests
    @Test
    void shouldScoreCurrentDateAs100() {
        Instant now = Instant.now();
        assertEquals(100, strategy.calculateDateRecencyScoreStepped(now));
    }

    @Test
    void shouldScore6MonthsAgoAs100() {
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        assertEquals(100, strategy.calculateDateRecencyScoreStepped(sixMonthsAgo));
    }

    @Test
    void shouldScore1YearAgoAs90() {
        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        assertEquals(90, strategy.calculateDateRecencyScoreStepped(oneYearAgo));
    }

    @Test
    void shouldScore2YearsAgoAs80() {
        Instant now = Instant.now();
        Instant twoYearsAgo = now.atZone(java.time.ZoneId.systemDefault())
                .toLocalDate().minusYears(2)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        assertEquals(80, strategy.calculateDateRecencyScoreStepped(twoYearsAgo));
    }

    @Test
    void shouldScore5YearsAgoAs50() {
        Instant now = Instant.now();
        Instant fiveYearsAgo = now.atZone(java.time.ZoneId.systemDefault())
                .toLocalDate().minusYears(5)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        assertEquals(50, strategy.calculateDateRecencyScoreStepped(fiveYearsAgo));
    }

    @Test
    void shouldScore10YearsAgoAs0() {
        Instant now = Instant.now();
        Instant tenYearsAgo = now.atZone(java.time.ZoneId.systemDefault())
                .toLocalDate().minusYears(10)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        assertEquals(0, strategy.calculateDateRecencyScoreStepped(tenYearsAgo));
    }

    @Test
    void shouldScore15YearsAgoAs0() {
        Instant fifteenYearsAgo = Instant.now().minus(5475, ChronoUnit.DAYS);
        assertEquals(0, strategy.calculateDateRecencyScoreStepped(fifteenYearsAgo));
    }

    @Test
    void shouldThrowExceptionForNullDate() {
        assertThrows(ScoringStrategyException.class,
                () -> strategy.calculateDateRecencyScoreStepped(null));
    }

    @Test
    void shouldThrowExceptionForFutureDate() {
        Instant futureDate = Instant.now().plus(365, ChronoUnit.DAYS);
        assertThrows(ScoringStrategyException.class,
                () -> strategy.calculateDateRecencyScoreStepped(futureDate));
    }

    // Integration Test for calculateScore
    @Test
    void shouldCalculateCompleteScore() {
        Instant now = Instant.now();
        Instant updatedRecently = now.minus(30, ChronoUnit.DAYS);

        GithubRepositoryDto repo = new GithubRepositoryDto(
                "test-repo",
                "Test repository",
                "https://github.com/test/test-repo",
                "Java",
                now.minus(365, ChronoUnit.DAYS), // created 1 year ago
                updatedRecently,  // updated 30 days ago
                updatedRecently,  // pushed 30 days ago
                50,  // stars
                10,  // watchers
                25,  // forks
                false,
                false,
                false,
                "MIT"
        );

        ScoreDto score = strategy.calculateScore(repo, 100, 100, 100);

        assertNotNull(score);

        // Verify score is within valid range
        assertTrue(score.getTotalScore() >= 0 && score.getTotalScore() <= 100);
        assertTrue(score.getRecencyScore() >= 0 && score.getRecencyScore() <= 100);
        assertTrue(score.getForksScore() >= 0 && score.getForksScore() <= 100);
        assertTrue(score.getStarGazersScore() >= 0 && score.getStarGazersScore() <= 100);

        // With 50 stars out of 100 max, should be 50
        assertEquals(50, score.getStarGazersScore());

        // With 25 forks out of 100 max, should be 25
        assertEquals(25, score.getForksScore());

        // Updated 30 days ago (less than 1 year) should be 100
        assertEquals(100, score.getRecencyScore());

        // Average of (100 + 50 + 25) / 3 = 58
        assertEquals(58, score.getTotalScore());
    }
}
