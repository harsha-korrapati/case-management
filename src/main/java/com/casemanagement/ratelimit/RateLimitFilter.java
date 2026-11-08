package com.casemanagement.ratelimit;

import com.casemanagement.dto.response.ApiErrorResponse;
import com.casemanagement.exception.RateLimitUnavailableException;
import com.casemanagement.security.AuthenticatedUser;
import com.casemanagement.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";

    private final RateLimitService rateLimitService;

    private final JsonMapper jsonMapper;

    @Value("${app.rate-limit.user-requests-per-minute:60}")
    private int userRequestLimit;

    @Value("${app.rate-limit.login-requests-per-minute:10}")
    private int loginRequestLimit;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Login always uses IP-based rate limiting.
        if (isLoginRequest(request)) {

            if (!applyIpRateLimit(request, response)) {
                return;
            }

            if (response.isCommitted()) {
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Authenticated requests use the JWT subject (user UUID).
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {

            String userId = authenticatedUser.getId().toString();

            boolean allowed = checkLimit(
                    "rate-limit:user:" + userId,
                    userRequestLimit,
                    request,
                    response
            );

            if (!allowed) {
                return;
            }

        } else {

            // All other unauthenticated requests use the client IP.
            if (!applyIpRateLimit(request, response)) {
                return;
            }

            if (response.isCommitted()) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean applyIpRateLimit(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String clientIp = request.getRemoteAddr();

        return checkLimit(
                "rate-limit:ip:" + clientIp,
                loginRequestLimit,
                request,
                response
        );
    }

    private boolean isLoginRequest(HttpServletRequest request) {

        return HttpMethod.POST.matches(request.getMethod()) && LOGIN_PATH.equals(request.getServletPath());
    }

    private boolean checkLimit(String key, int limit, HttpServletRequest request, HttpServletResponse response) throws IOException {

        RateLimitDecision decision;

        try {
            decision = rateLimitService.checkRateLimit(key, limit);

        } catch (RateLimitUnavailableException exception) {

            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiErrorResponse error = new ApiErrorResponse(
                    Instant.now(),
                    503,
                    "Service Unavailable",
                    "Rate limiting service is temporarily unavailable. Please try again later.",
                    request.getRequestURI()
            );

            jsonMapper.writeValue(response.getOutputStream(), error);

            return false;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit())
        );

        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiErrorResponse error = new ApiErrorResponse(
                    Instant.now(),
                    429,
                    "Too Many Requests",
                    "Rate limit exceeded. Please try again later.",
                    request.getRequestURI()
            );

            jsonMapper.writeValue(response.getOutputStream(), error);

            return false;
        }

        return true;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/");
    }

}
