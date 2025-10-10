package tech.alaz.git.project.score.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
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
                .thenReturn(Mono.just(expectedResponse));

        Mono<ScoredGitHubRepoSearchResponseDto> result = controller.searchAndScoreGitRepositories(
                "Java", creationDate, 3, null, "test"
        );

        assertNotNull(result);
        ScoredGitHubRepoSearchResponseDto response = result.block();
        assertNotNull(response);
        assertEquals(1, response.totalResultCount());
        assertEquals(expectedResponse, response);
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
                .thenReturn(Mono.just(expectedResponse));

        Mono<ScoredGitHubRepoSearchResponseDto> result = controller.searchAndScoreGitRepositories("Java", creationDate, maxPageSize, null, "test");
        assertNotNull(result);
        assertDoesNotThrow(() -> result.block());
    }
}
