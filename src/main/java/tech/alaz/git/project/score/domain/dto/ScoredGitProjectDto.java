package tech.alaz.git.project.score.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScoredGitProjectDto(
        @JsonProperty("repository")
        GithubRepositoryDto repository,

        @JsonProperty("score")
        ScoreDto score) {
}
