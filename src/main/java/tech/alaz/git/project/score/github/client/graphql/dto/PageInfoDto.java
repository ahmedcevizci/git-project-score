package tech.alaz.git.project.score.github.client.graphql.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents pagination information from GitHub GraphQL API
 * following the Relay Cursor Connections Specification.
 *
 * @see <a href="https://relay.dev/graphql/connections.htm">GraphQL Cursor Connections Specification</a>
 */
public record PageInfoDto(
        /** Indicates whether more pages exist following the current page */
        @JsonProperty("hasNextPage")
        boolean hasNextPage,

        /** Indicates whether more pages exist prior to the current page */
        @JsonProperty("hasPreviousPage")
        boolean hasPreviousPage,

        /** Cursor pointing to the start of the current page */
        @JsonProperty("startCursor")
        String startCursor,

        /** Cursor pointing to the end of the current page - use this for the next request */
        @JsonProperty("endCursor")
        String endCursor
) {
}
