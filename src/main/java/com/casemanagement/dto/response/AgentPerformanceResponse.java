package com.casemanagement.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AgentPerformanceResponse(
        UUID agentId,
        String email,
        Long totalCasesCreated,
        Long totalCasesResolved,
        BigDecimal averageTimeToResolveSeconds
) {
}
