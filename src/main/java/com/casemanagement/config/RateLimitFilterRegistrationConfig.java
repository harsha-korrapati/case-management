package com.casemanagement.config;

import com.casemanagement.ratelimit.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RateLimitFilterRegistrationConfig {

    private final RateLimitFilter rateLimitFilter;

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {

        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);

        registration.setEnabled(false);

        return registration;
    }
}
