package tech.alaz.git.project.score.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebInputException;
import tech.alaz.git.project.score.api.controller.exception.CreationDateCannotBeInFutureException;
import tech.alaz.git.project.score.api.controller.exception.PageSizeCannotExceedMaxValueException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandlePageSizeCannotExceedMaxValueException() {
        PageSizeCannotExceedMaxValueException exception =
                new PageSizeCannotExceedMaxValueException(10, "Page size cannot exceed 10");

        ResponseEntity<Map<String, Object>> response = handler.handlePageSizeCannotExceedMaxValueException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Page size cannot exceed 10", response.getBody().get("message"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }

    @Test
    void shouldHandleCreationDateCannotBeInFutureException() {
        CreationDateCannotBeInFutureException exception =
                new CreationDateCannotBeInFutureException("Creation date must be in the past");

        ResponseEntity<Map<String, Object>> response = handler.handleCreationDateCannotBeInFutureException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Creation date must be in the past", response.getBody().get("message"));
    }

    @Test
    void shouldHandleServerWebInputException() {
        ServerWebInputException exception = new ServerWebInputException("Invalid date format");

        ResponseEntity<Map<String, Object>> response = handler.handleServerWebInputException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Invalid argument provided", response.getBody().get("message"));
    }

    @Test
    void shouldHandleGenericException() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }
}
