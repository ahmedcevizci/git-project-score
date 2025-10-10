package tech.alaz.git.project.score.api.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import tech.alaz.git.project.score.domain.dto.ScoredGitProjectDto;
import tech.alaz.git.project.score.github.client.graphql.dto.PageInfoDto;

import java.util.List;

public record ScoredGitHubRepoSearchResponseDto(

        @JsonProperty("totalResultCount")
        int totalResultCount,

        @JsonProperty("maxStarGazersCount")
        int maxStarGazersCount,

        @JsonProperty("maxForkCount")
        int maxForkCount,

        @JsonProperty("pageInfo")
        PageInfoDto pageInfo,

        @JsonProperty("githubRepositories")
        List<ScoredGitProjectDto> githubRepositories) {
}