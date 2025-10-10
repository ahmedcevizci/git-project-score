package tech.alaz.git.project.score.domain.score.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.domain.dto.ScoreDto;
import tech.alaz.git.project.score.domain.exception.ScoringStrategyException;
import tech.alaz.git.project.score.domain.score.ScoreOverHundred;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DefaultScoringStrategy implements ScoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DefaultScoringStrategy.class);

    @Override
    public boolean isRelativeToOtherProjects() {
        return true;
    }

    @Override
    public ScoreDto calculateScore(GithubRepositoryDto githubRepositoryDto, Integer totalMatchingRepoCount, Integer maxForkCount, Integer maxStarGazersCount) {
        logger.debug("Calculating score for repository: {} (stars: {}, forks: {}, maxStars: {}, maxForks: {})",
                githubRepositoryDto.name(), githubRepositoryDto.stargazerCount(), githubRepositoryDto.forkCount(),
                maxStarGazersCount, maxForkCount);

        ScoreOverHundred recencyScore = new ScoreOverHundred(calculateDateRecencyScoreStepped(githubRepositoryDto.updatedAt()));
        ScoreOverHundred starScore = new ScoreOverHundred(calculateStarSuccessScoreLinear(githubRepositoryDto.stargazerCount(), maxStarGazersCount));
        ScoreOverHundred forkScore = new ScoreOverHundred(calculateForkSuccessScoreLinear(githubRepositoryDto.forkCount(), maxForkCount));
        ScoreOverHundred totalScore = calculateAverage(recencyScore, starScore, forkScore);

        logger.debug("Calculated scores for {}: total={}, recency={}, stars={}, forks={}",
                githubRepositoryDto.name(), totalScore.getScore(), recencyScore.getScore(),
                starScore.getScore(), forkScore.getScore());
        return new ScoreDto(totalScore, recencyScore, starScore, forkScore);
    }

    private ScoreOverHundred calculateAverage(ScoreOverHundred recencyScore, ScoreOverHundred starScore, ScoreOverHundred forkScore) {
        int avgScore = (recencyScore.getScore() + starScore.getScore() + forkScore.getScore()) / 3;
        return new ScoreOverHundred(avgScore);
    }

    /**
     * Calculates a success score (0-100) using a linear scale based on the repository's
     * star count relative to the maximum star count.
     *
     * @param repoStarCount the star count of the repository to score
     * @param maxStarCount  the maximum star count in the dataset
     * @return an integer percentage from 0 to 100
     */
    public int calculateStarSuccessScoreLinear(int repoStarCount, int maxStarCount) {
        if (maxStarCount < repoStarCount) {
            throw new IllegalStateException("maxStarCount cannot be lower than repoStartCount. maxStarCount=" + maxStarCount + ", repoStarCount=" + repoStarCount);
        }

        if (maxStarCount < 0) {
            throw new ScoringStrategyException("maxStarCount must be positive, got: " + maxStarCount);
        }
        if (repoStarCount < 0) {
            throw new ScoringStrategyException("repoStarCount must be non-negative, got: " + repoStarCount);
        }

        if (repoStarCount >= maxStarCount) {
            return 100;
        }

        double percentage = ((double) repoStarCount / maxStarCount) * 100.0;
        return (int) Math.round(percentage);
    }

    /**
     * Calculates a success score (0-100) using a linear scale based on the repository's
     * fork count relative to the maximum fork count.
     *
     * @param repoForkCount the fork count of the repository to score
     * @param maxForkCount  the maximum fork count in the dataset
     * @return an integer percentage from 0 to 100
     */
    public int calculateForkSuccessScoreLinear(int repoForkCount, int maxForkCount) {
        if (maxForkCount < repoForkCount) {
            throw new IllegalStateException("maxForkCount cannot be lower than repoForkCount. maxForkCount=" + maxForkCount + ", repoForkCount=" + repoForkCount);
        }

        if (maxForkCount < 0) {
            throw new ScoringStrategyException("maxForkCount must be positive, got: " + maxForkCount);
        }
        if (repoForkCount < 0) {
            throw new ScoringStrategyException("repoForkCount must be non-negative, got: " + repoForkCount);
        }

        if (repoForkCount >= maxForkCount) {
            return 100;
        }

        double percentage = ((double) repoForkCount / maxForkCount) * 100.0;
        return (int) Math.round(percentage);
    }

    /**
     * Calculates a score from 0 to 10 based on how recent the given date is.
     * - More than 10 years ago: score = 0
     * - Between 9-10 years ago: score = 10
     * - Between 8-9 years ago: score = 20
     * - Between 7-8 years ago: score = 30
     * - ...
     * - Between 1-2 years ago: score = 80
     * - Between 0-1 years ago: score = 90
     * - Current year (less than 1 year): score = 100
     *
     * @param instant the date to score
     * @return an integer score from 0 to 100 (inclusive)
     */
    public int calculateDateRecencyScoreStepped(Instant instant) {
        if (instant == null) {
            throw new ScoringStrategyException("Date cannot be null");
        }

        Instant now = Instant.now();

        if (instant.isAfter(now)) {
            throw new ScoringStrategyException("Date cannot be in the future");
        }

        // Convert Instant to LocalDate for calendar-based year calculation
        LocalDate instantDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate nowDate = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        long yearsDifference = ChronoUnit.YEARS.between(instantDate, nowDate);

        // More than 10 years ago
        if (yearsDifference >= 10) {
            return 0;
        }

        // Convert year difference to score
        // 9-10 years = 10, 8-9 years = 20, ..., 0-1 years = 100
        return (10 - (int) yearsDifference) * 10;
    }
}
