package tech.alaz.git.project.score.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tech.alaz.git.project.score.api.controller.dto.ScoredGitHubRepoSearchResponseDto;
import tech.alaz.git.project.score.api.controller.exception.CreationDateCannotBeInFutureException;
import tech.alaz.git.project.score.api.controller.exception.PageSizeCannotExceedMaxValueException;
import tech.alaz.git.project.score.domain.dto.GithubRepositoryDto;
import tech.alaz.git.project.score.domain.dto.ScoreDto;
import tech.alaz.git.project.score.domain.dto.ScoredGitProjectDto;
import tech.alaz.git.project.score.github.service.GitHubRepoSearchService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static tech.alaz.git.project.score.domain.score.ScoreOverHundred.score;

class GitProjectScoreControllerTest {

    @Mock
    private GitHubRepoSearchService gitHubRepoSearchService;

    private GitProjectScoreController controller;
    private final int maxPageSize = 5;
    private final LocalDate creationDate = LocalDate.of(2020, 1, 1);
    private final Instant now = Instant.now();
    private final Instant updatedRecently = now.minus(30, ChronoUnit.DAYS);
    private final GithubRepositoryDto githubRepositoryDto = new GithubRepositoryDto(
            "test-repo",
            "Test repository",
            "https://github.com/test/test-repo",
            "Java",
            now.minus(365, ChronoUnit.DAYS),
            updatedRecently,
            updatedRecently,
            50,
            10,
            25,
            false,
            false,
            false,
            "MIT"
    );
    // Average of (100 + 50 + 25) / 3 = 58.3
    private final ScoreDto scoreDto = new ScoreDto(score(58), score(100), score(50), score(25));

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new GitProjectScoreController(gitHubRepoSearchService, maxPageSize);
    }

    @Test
    void shouldSearchAndScoreGitRepositoriesSuccessfully() {
        ScoredGitHubRepoSearchResponseDto expectedResponse =
                new ScoredGitHubRepoSearchResponseDto(1, 100, 50, null,
                        List.of(new ScoredGitProjectDto(githubRepositoryDto, scoreDto)));

        when(gitHubRepoSearchService.searchAndScoreGitHubRepos(
                any(), any(), any(LocalDate.class), anyInt(), any(), any()))
                .thenReturn(expectedResponse);

        ScoredGitHubRepoSearchResponseDto result = controller.searchAndScoreGitRepositories(
                "Java", creationDate, 3, null, "test"
        );

        assertNotNull(result);
        assertEquals(1, result.totalResultCount());
        assertEquals(expectedResponse, result);
    }

    @Test
    void shouldThrowExceptionWhenPageSizeExceedsMax() {
        assertThrows(PageSizeCannotExceedMaxValueException.class, () ->
                controller.searchAndScoreGitRepositories("Java", creationDate, 6, null, "test")
        );
    }

    @Test
    void shouldThrowExceptionWhenCreationDateIsNull() {
        assertThrows(CreationDateCannotBeInFutureException.class, () ->
                controller.searchAndScoreGitRepositories("Java", null, 3, null, "test")
        );
    }

    @Test
    void shouldThrowExceptionWhenCreationDateIsInFuture() {
        LocalDate futureDate = LocalDate.now().plusDays(1);

        assertThrows(CreationDateCannotBeInFutureException.class, () ->
                controller.searchAndScoreGitRepositories("Java", futureDate, 3, null, "test")
        );
    }

    @Test
    void shouldAcceptValidPageSize() {
        ScoredGitHubRepoSearchResponseDto expectedResponse =
                new ScoredGitHubRepoSearchResponseDto(10, 100, 50, null, List.of());

        when(gitHubRepoSearchService.searchAndScoreGitHubRepos(
                any(), any(), any(LocalDate.class), anyInt(), any(), any()))
                .thenReturn(expectedResponse);

        ScoredGitHubRepoSearchResponseDto result = controller.searchAndScoreGitRepositories(
                "Java", creationDate, maxPageSize, null, "test");
        assertNotNull(result);
    }
}