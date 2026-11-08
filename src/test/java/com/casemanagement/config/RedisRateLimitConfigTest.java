package com.casemanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisRateLimitConfigTest {

    @Test
    void rateLimitScriptLoadsLuaResourceAndUsesListResultType() throws Exception {
        DefaultRedisScript<List> script = new RedisRateLimitConfig().rateLimitScript();

        assertEquals(List.class, script.getResultType());
        assertTrue(script.getScriptAsString().contains("INCR"));
        assertTrue(new ClassPathResource("scripts/rate_limit.lua").exists());
    }
}
