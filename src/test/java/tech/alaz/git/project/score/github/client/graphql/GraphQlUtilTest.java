package tech.alaz.git.project.score.github.client.graphql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphQlUtilTest {

    @Test
    void shouldLoadGraphQLQuerySuccessfully() {
        String query = GraphQlUtil.loadGraphQLQuery("GitHubReposSearchQuery.graphql");

        assertNotNull(query);
        assertFalse(query.isEmpty());
        assertTrue(query.length() > 50, "Query should have substantial content");
        assertTrue(query.contains("query") || query.contains("Query"));
    }

    @Test
    void shouldThrowExceptionForNonExistentFile() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> GraphQlUtil.loadGraphQLQuery("NonExistent.graphql"));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Failed to load GraphQL query"));
    }

    @Test
    void shouldThrowExceptionForNullFileName() {
        assertThrows(Exception.class,
                () -> GraphQlUtil.loadGraphQLQuery(null));
    }

    @Test
    void shouldThrowExceptionForEmptyFileName() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> GraphQlUtil.loadGraphQLQuery(""));

        assertNotNull(exception.getMessage());
    }

    @Test
    void shouldThrowExceptionForFileWithoutExtension() {
        // This should fail since file extension is not assumed.
        assertThrows(RuntimeException.class,
                () -> GraphQlUtil.loadGraphQLQuery("GitHubReposSearchQuery"));
    }
}
