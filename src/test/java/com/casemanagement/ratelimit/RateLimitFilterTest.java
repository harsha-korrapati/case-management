package com.casemanagement.ratelimit;

import com.casemanagement.dto.response.ApiErrorResponse;
import com.casemanagement.exception.RateLimitUnavailableException;
import com.casemanagement.security.AuthenticatedUser;
import com.casemanagement.service.RateLimitService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private JsonMapper jsonMapper;
    @Mock
    private FilterChain filterChain;
    @InjectMocks
    private RateLimitFilter filter;

    @BeforeEach
    void setLimits() {
        ReflectionTestUtils.setField(filter, "userRequestLimit", 60);
        ReflectionTestUtils.setField(filter, "loginRequestLimit", 10);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowedLoginUsesIpKeyAndContinues() throws Exception {
        ReflectionTestUtils.setField(filter, "loginRequestLimit", 10);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login");
        request.setRemoteAddr("192.0.2.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.checkRateLimit("rate-limit:ip:192.0.2.7", 10))
                .thenReturn(new RateLimitDecision(true, 10, 9, 60));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("10", response.getHeader("X-RateLimit-Limit"));
        assertEquals("9", response.getHeader("X-RateLimit-Remaining"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void deniedRequestReturns429WithoutContinuing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cases");
        request.setServletPath("/cases");
        request.setRemoteAddr("192.0.2.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.checkRateLimit("rate-limit:ip:192.0.2.9", 10))
                .thenReturn(new RateLimitDecision(false, 10, 0, 30));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("30", response.getHeader("Retry-After"));
        verify(jsonMapper).writeValue(any(OutputStream.class), any(ApiErrorResponse.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void authenticatedRequestUsesUserSpecificKey() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = () -> userId;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cases");
        request.setServletPath("/cases");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.checkRateLimit("rate-limit:user:" + userId, 60))
                .thenReturn(new RateLimitDecision(true, 60, 59, 60));

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).checkRateLimit("rate-limit:user:" + userId, 60);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void unavailableRateLimitReturns503WithoutContinuing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cases");
        request.setServletPath("/cases");
        request.setRemoteAddr("192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.checkRateLimit("rate-limit:ip:192.0.2.10", 10))
                .thenThrow(new RateLimitUnavailableException("unavailable", new IllegalStateException()));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(503, response.getStatus());
        verify(jsonMapper).writeValue(any(OutputStream.class), any(ApiErrorResponse.class));
        verify(filterChain, never()).doFilter(request, response);
    }
}
