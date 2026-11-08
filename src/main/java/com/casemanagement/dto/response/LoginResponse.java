package com.casemanagement.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}

