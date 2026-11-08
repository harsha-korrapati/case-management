package com.casemanagement.service.impl;

import com.casemanagement.exception.RateLimitUnavailableException;
import com.casemanagement.ratelimit.RateLimitDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private DefaultRedisScript<List> script;

    @InjectMocks
    private RateLimitServiceImpl service;

    @Test
    void checkRateLimitCalculatesRemainingAndRetryAfter() {
        when(redisTemplate.execute(script, List.of("client:1"))).thenReturn(List.of(3L, 1501L));

        RateLimitDecision decision = service.checkRateLimit("client:1", 5);

        assertTrue(decision.allowed());
        assertEquals(5, decision.limit());
        assertEquals(2, decision.remaining());
        assertEquals(2, decision.retryAfterSeconds());
    }

    @Test
    void checkRateLimitDeniesRequestsOverLimitAndClampsRemainingToZero() {
        when(redisTemplate.execute(script, List.of("client:1"))).thenReturn(List.of(6L, 0L));

        RateLimitDecision decision = service.checkRateLimit("client:1", 5);

        assertFalse(decision.allowed());
        assertEquals(0, decision.remaining());
        assertEquals(1, decision.retryAfterSeconds());
    }

    @Test
    void checkRateLimitWrapsInvalidRedisResponseAsUnavailable() {
        when(redisTemplate.execute(script, List.of("client:1"))).thenReturn(List.of(1L));

        assertThrows(RateLimitUnavailableException.class,
                () -> service.checkRateLimit("client:1", 5));
    }
}
