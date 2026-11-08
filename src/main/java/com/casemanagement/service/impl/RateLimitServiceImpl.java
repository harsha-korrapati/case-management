package com.casemanagement.service.impl;

import com.casemanagement.exception.RateLimitUnavailableException;
import com.casemanagement.ratelimit.RateLimitDecision;
import com.casemanagement.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<List> rateLimitScript;

    @Override
    public RateLimitDecision checkRateLimit(String key, int limit) {

        try {
            List<?> result = redisTemplate.execute(rateLimitScript, List.of(key));

            if (result == null || result.size() < 2) {
                throw new IllegalStateException("Invalid response from Redis rate limit script");
            }

            long requestCount = ((Number) result.get(0)).longValue();
            long ttlMillis = ((Number) result.get(1)).longValue();

            long remaining = Math.max(0, limit - requestCount);

            long retryAfterSeconds = Math.max(1, (long) Math.ceil(ttlMillis / 1000.0));

            return new RateLimitDecision(
                    requestCount <= limit,
                    limit,
                    remaining,
                    retryAfterSeconds
            );

        } catch (Exception exception) {
            throw new RateLimitUnavailableException("Rate limiting service is unavailable", exception);
        }
    }
}
