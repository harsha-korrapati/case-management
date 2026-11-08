package com.casemanagement.repository;

import com.casemanagement.entity.Case;
import com.casemanagement.entity.CaseEvent;
import com.casemanagement.entity.OutboxEvent;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CaseEventType;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentPerformanceRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private AgentPerformanceRepository agentPerformanceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void calculatesCreatedAndResolvedCaseCountsForAgent() {
        User agent = userRepository.saveAndFlush(
                new User(UUID.randomUUID() + "@example.com", "hashed", UserRole.AGENT));
        Case createdCase = caseRepository.saveAndFlush(
                new Case("Performance case", "Description", CasePriority.HIGH, agent));
        outboxEventRepository.saveAndFlush(new OutboxEvent(
                new CaseEvent(UUID.randomUUID(), CaseEventType.CASE_STATUS_CHANGED,
                        createdCase.getId(), Instant.now(), Map.of(), "performance-test"),
                "{\"payload\":{\"status\":\"RESOLVED\"}}"));

        var row = agentPerformanceRepository.findAgentPerformance(null, null).stream()
                .filter(performance -> agent.getEmail().equals(performance.getEmail()))
                .findFirst()
                .orElseThrow();

        assertEquals(1L, row.getTotalCasesCreated());
        assertEquals(1L, row.getTotalCasesResolved());
    }
}
