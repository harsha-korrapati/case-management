package com.casemanagement.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface AgentPerformanceProjection {

    byte[] getAgentId();

    String getEmail();

    Long getTotalCasesCreated();

    Long getTotalCasesResolved();

    BigDecimal getAverageResolutionSeconds();
}
