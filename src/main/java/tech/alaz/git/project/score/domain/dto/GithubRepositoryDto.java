package tech.alaz.git.project.score.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Represents a GitHub repository and its properties related to its popularity.
 */
public record GithubRepositoryDto(

        /** The name of the repository (e.g., "hello-world") */
        @JsonProperty("name")
        String name,

        /** A short description of the repository */
        @JsonProperty("description")
        String description,

        // URLs for Access
        /** The URL to view the repository on GitHub.com */
        @JsonProperty("htmlUrl")
        String htmlUrl,

        /** Primary programming language */
        @JsonProperty("primaryLanguage")
        String primaryLanguage,

        // Timestamps
        /** When the repository was created */
        @JsonProperty("createdAt")
        Instant createdAt,

        /** When the repository was last updated (any change) */
        @JsonProperty("updatedAt")
        Instant updatedAt,

        /** When the last push/commit was made */
        @JsonProperty("pushedAt")
        Instant pushedAt,

        // Additional Metadata
        /** Number of users who have starred this repository */
        @JsonProperty("stargazerCount")
        Integer stargazerCount,

        /** Number of users watching this repository */
        @JsonProperty("watcherCount")
        Integer watcherCount,

        // Repository Statistics
        /** Number of times this repository has been forked */
        @JsonProperty("forkCount")
        Integer forkCount,

        /** Whether this repository is a fork of another repository */
        @JsonProperty("isFork")
        Boolean isFork,

        // Repository State
        /** Whether the repository is archived (read-only) */
        @JsonProperty("isArchived")
        Boolean isArchived,

        /** Whether the repository has been disabled */
        @JsonProperty("isDisabled")
        Boolean isDisabled,

        /** Human-readable full name of the license (e.g., "MIT License", "Apache License 2.0") */
        @JsonProperty("licenseName")
        String licenseName
) {
}
