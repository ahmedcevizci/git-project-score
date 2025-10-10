package tech.alaz.git.project.score.github.client.graphql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;

public class GraphQlUtil {

    private static final Logger logger = LoggerFactory.getLogger(GraphQlUtil.class);

    public static String loadGraphQLQuery(String fileName) {
        logger.debug("Loading GraphQL query from file: {}", fileName);
        try {
            var resource = new ClassPathResource("graphql/" + fileName);
            String query = Files.readString(resource.getFile().toPath());
            logger.debug("Successfully loaded GraphQL query from `{}`", fileName);
            return query;
        } catch (Exception e) {
            logger.error("Failed to load GraphQL query file: {}", fileName, e);
            throw new RuntimeException("Failed to load GraphQL query: " + fileName, e);
        }
    }
}
