package com.casemanagement.service;

import com.casemanagement.ratelimit.RateLimitDecision;

public interface RateLimitService {

    RateLimitDecision checkRateLimit(String key, int limit);

}
