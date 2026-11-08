package com.casemanagement.ratelimit;

public record RateLimitDecision(
        boolean allowed,
        int limit,
        long remaining,
        long retryAfterSeconds
) {
}
