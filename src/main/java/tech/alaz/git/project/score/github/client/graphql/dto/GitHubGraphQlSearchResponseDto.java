package tech.alaz.git.project.score.github.client.graphql.dto;

import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;

import java.util.List;

public record GitHubGraphQlSearchResponseDto(

        int totalResultCount,

        PageInfoDto pageInfo,

        List<GithubRepositoryDto> repositories) {
}