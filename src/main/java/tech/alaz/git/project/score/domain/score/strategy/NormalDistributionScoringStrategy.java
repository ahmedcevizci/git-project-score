package tech.alaz.git.project.score.domain.score.strategy;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.domain.dto.ScoreDto;

// TODO Implement this as an advanced scoring strategy
public class NormalDistributionScoringStrategy implements ScoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(NormalDistributionScoringStrategy.class);

    public NormalDistributionScoringStrategy() {
        logger.error("Attempting to instantiate NormalDistributionScoringStrategy which is not yet implemented");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean isRelativeToOtherProjects() {
        return true;
    }

    @Override
    public ScoreDto calculateScore(GithubRepositoryDto githubRepositoryDto, Integer totalMatchingRepoCount, Integer maxForkCount, Integer maxStarGazersCount) {
        logger.error("calculateScore called on unimplemented NormalDistributionScoringStrategy");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Because a normal distribution extends infinitely, we'll assume:
     * The mean (μ) is at half the maximum: μ = max / 2
     * The standard deviation (σ) is such that almost all values fall within [0, max] (≈99.7%), meaning:
     * σ = max / 6
     * (since ±3σ covers 99.7% in a normal distribution)
     * The "three-sigma rule":
     * In a normal (bell curve) distribution, about:
     * 68% of values lie within ±1σ (one standard deviation) https://en.wikipedia.org/wiki/Standard_deviation
     * 95% within ±2σ
     * 99.7% within ±3σ
     */
    public static double calculateNormallyDistributedPercentage(double maxMeasurement, double measurement) {
        logger.debug("Calculating normally distributed percentage: measurement={}, maxMeasurement={}", measurement, maxMeasurement);

        if (maxMeasurement < 0) {
            logger.error("Invalid maxMeasurement: {}", maxMeasurement);
            throw new IllegalArgumentException("maxMeasurement must be positive, got: " + maxMeasurement);
        }
        if (measurement < 0) {
            logger.error("Invalid measurement: {}", measurement);
            throw new IllegalArgumentException("measurement must be non-negative, got: " + measurement);
        }

        double mean = maxMeasurement / 2.0;
        double standardDeviation = maxMeasurement / 6.0;
        logger.trace("Using normal distribution with mean={}, standardDeviation={}", mean, standardDeviation);

        NormalDistribution normalDist = new NormalDistribution(mean, standardDeviation);
        double percentile = normalDist.cumulativeProbability(measurement) * 100.0;

        logger.debug("Measurement {} is at {}% percentile (max = {})", measurement, percentile, maxMeasurement);

        return percentile;
    }
}
