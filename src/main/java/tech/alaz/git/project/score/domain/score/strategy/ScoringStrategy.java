package tech.alaz.git.project.score.domain.score.strategy;

import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.domain.dto.ScoreDto;

public interface ScoringStrategy {

    boolean isRelativeToOtherProjects();

    ScoreDto calculateScore(GithubRepositoryDto githubRepositoryDto, Integer totalMatchingRepoCount, Integer maxForkCount, Integer maxStarGazersCount);

}
