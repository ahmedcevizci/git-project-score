package tech.alaz.git.project.score.api.controller;

import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.alaz.git.project.score.api.controller.dto.ScoredGitHubRepoSearchResponseDto;
import tech.alaz.git.project.score.api.controller.exception.CreationDateCannotBeInFutureException;
import tech.alaz.git.project.score.api.controller.exception.PageSizeCannotExceedMaxValueException;
import tech.alaz.git.project.score.domain.score.strategy.DefaultScoringStrategy;
import tech.alaz.git.project.score.github.service.GitHubRepoSearchService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/search")
public class GitProjectScoreController {

    private static final Logger logger = LoggerFactory.getLogger(GitProjectScoreController.class);

    private final int maxPageSize;

    public GitHubRepoSearchService gitHubRepoSearchService;

    public GitProjectScoreController(GitHubRepoSearchService gitHubRepoSearchService, @Value("${git-project-score.max-search-page-size:5}") int maxPageSize) {
        this.gitHubRepoSearchService = gitHubRepoSearchService;
        this.maxPageSize = maxPageSize;
    }

    @GetMapping
    public ScoredGitHubRepoSearchResponseDto searchAndScoreGitRepositories(
            @RequestParam String language,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate creationDate,
            @RequestParam int pageSize,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, value = "searchInNames") @Size(max = 20) String searchInNames) {

        logger.info("Received search request: language={}, creationDate={}, pageSize={}, cursor={}, searchInNames={}",
                language, creationDate, pageSize, cursor != null ? "present" : "null", searchInNames);

        if (pageSize > maxPageSize) {
            logger.warn("Page size {} exceeds maximum allowed size {}", pageSize, maxPageSize);
            throw new PageSizeCannotExceedMaxValueException(maxPageSize, "`pageSize` query parameter cannot exceed " + maxPageSize);
        }

        if (creationDate == null || creationDate.isAfter(LocalDate.now())) {
            logger.warn("Invalid creation date: {}", creationDate);
            throw new CreationDateCannotBeInFutureException("`creationDate` query parameter must be a valid date in the past");
        }

        ScoredGitHubRepoSearchResponseDto response = gitHubRepoSearchService.searchAndScoreGitHubRepos(
                searchInNames, language, creationDate, pageSize, cursor, new DefaultScoringStrategy());

        logger.info("Successfully completed search request: returned {} repositories out of {} total",
                response.githubRepositories().size(), response.totalResultCount());
        return response;
    }
}