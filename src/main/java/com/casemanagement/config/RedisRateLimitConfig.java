package com.casemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
public class RedisRateLimitConfig {

    @Bean
    public DefaultRedisScript<List> rateLimitScript() {

        DefaultRedisScript<List> script = new DefaultRedisScript<>();

        script.setLocation(new ClassPathResource("scripts/rate_limit.lua"));

        script.setResultType(List.class);

        return script;
    }
}
