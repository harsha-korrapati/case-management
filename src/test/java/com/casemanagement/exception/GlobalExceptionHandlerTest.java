package com.casemanagement.exception;

import com.casemanagement.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundReturnsNotFoundPayloadWithRequestPath() {
        HttpServletRequest request = requestAt("/api/cases/123");

        var response = handler.handleNotFound(
                new ResourceNotFoundException("Case not found"), request);

        assertEquals(404, response.getStatusCode().value());
        ApiErrorResponse body = response.getBody();
        assertEquals(404, body.status());
        assertEquals("Not Found", body.error());
        assertEquals("Case not found", body.message());
        assertEquals("/api/cases/123", body.path());
    }

    @Test
    void handleBadCredentialsDoesNotExposeAuthenticationDetails() {
        var response = handler.handleBadCredentials(
                new BadCredentialsException("sensitive provider message"), requestAt("/auth/login"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid email or password", response.getBody().message());
        assertEquals("/auth/login", response.getBody().path());
    }

    @Test
    void handleDataIntegrityReturnsConflict() {
        var response = handler.handleDataIntegrity(
                new org.springframework.dao.DataIntegrityViolationException("duplicate"),
                requestAt("/api/users"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("The request conflicts with existing data", response.getBody().message());
    }

    private HttpServletRequest requestAt(String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }
}
