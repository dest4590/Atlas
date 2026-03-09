package org.collapseloader.atlas.exception;

import org.collapseloader.atlas.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private static class ValidationTarget {
    }

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleUnauthorizedExceptionReturns401() {
        ResponseEntity<ApiResponse<Void>> response = handler
                .handleUnauthorizedException(new UnauthorizedException("Unauthorized"));

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals(false, response.getBody().success());
    }

    @Test
    void handleAccessDeniedExceptionReturns403() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException();

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Access denied", response.getBody().error());
    }

    @Test
    void handleMethodNotSupportedReturns405() {
        ResponseEntity<ApiResponse<Void>> response = handler
                .handleMethodNotSupported(new HttpRequestMethodNotSupportedException("TRACE"));

        assertEquals(405, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().error().contains("Request method not supported"));
    }

    @Test
    void handleGenericExceptionReturns500() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleException(new RuntimeException("boom"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().error());
    }

    @Test
    void handleRuntimeExceptionReturns500() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(new RuntimeException("boom"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().error());
    }

    @Test
    void handleDatabaseExceptionReturns500() {
        var exception = new DataAccessException("db") {
        };

        ResponseEntity<ApiResponse<Void>> response = handler.handleDatabaseException(exception);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Database error", response.getBody().error());
    }

    @Test
    void handleChangeSetNotFoundFallsBackToDefaultMessage() {
        var notFound = new org.springframework.data.crossstore.ChangeSetPersister.NotFoundException();

        ResponseEntity<ApiResponse<Void>> response = handler.handleChangeSetNotFound(notFound);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Not found", response.getBody().error());
    }

    @Test
    void handleEntityNotFoundReturns404() {
        ResponseEntity<ApiResponse<Void>> response = handler
                .handleEntityNotFoundException(new EntityNotFoundException("missing"));

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("missing", response.getBody().error());
    }

    @Test
    void handleValidationExceptionReturns400() {
        ResponseEntity<ApiResponse<Void>> response = handler
                .handleValidationException(new ValidationException("invalid"));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("invalid", response.getBody().error());
    }

    @Test
    void handleForbiddenExceptionReturns403() {
        ResponseEntity<ApiResponse<Void>> response = handler
                .handleForbiddenException(new ForbiddenException("forbidden"));

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("forbidden", response.getBody().error());
    }

    @Test
    void handleConflictExceptionReturns409() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleConflictException(new ConflictException("conflict"));

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("conflict", response.getBody().error());
    }

    @Test
    void handleAuthenticationExceptionReturns401() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthenticationException();

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Authentication failed", response.getBody().error());
    }

    @Test
    void handleTitanExceptionReturns500WithMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleTitanException(new TitanException("titan failed"));

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("titan failed", response.getBody().error());
    }

    @Test
    void handleValidationExceptionsFromBindExceptionReturnsFieldMessages() {
        var bindException = new BindException(new ValidationTarget(), "request");
        bindException.rejectValue("email", "NotBlank", "must not be blank");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationExceptions(bindException);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().error().contains("email: must not be blank"));
    }
}
