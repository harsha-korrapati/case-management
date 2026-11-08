package com.casemanagement.repository;

import com.casemanagement.entity.Case;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.CaseStatus;
import com.casemanagement.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseReportRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private CaseReportRepository reportRepository;
    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void aggregatesStatusesAndPrioritiesWithinDateRangeAndExcludesDeletedCases() {
        User user = userRepository.saveAndFlush(
                new User(UUID.randomUUID() + "@example.com", "hashed", UserRole.AGENT));
        Case active = caseRepository.saveAndFlush(
                new Case("Report active", "Description", CasePriority.HIGH, user));
        Case deleted = new Case("Report deleted", "Description", CasePriority.LOW, user);
        deleted.markDeleted(Instant.now());
        caseRepository.saveAndFlush(deleted);
        Instant from = Instant.now().minusSeconds(10);
        Instant to = Instant.now().plusSeconds(10);

        var statuses = reportRepository.countCasesByStatus(from, to);
        var priorities = reportRepository.countCasesByPriority(from, to);

        assertTrue(statuses.stream().anyMatch(row ->
                row.getCategory().equals(CaseStatus.OPEN.name()) && row.getTotal() == 1L));
        assertTrue(priorities.stream().anyMatch(row ->
                row.getCategory().equals(CasePriority.HIGH.name()) && row.getTotal() == 1L));
        assertEquals(active.getStatus().name(), statuses.stream()
                .filter(row -> row.getCategory().equals(CaseStatus.OPEN.name()))
                .findFirst().orElseThrow().getCategory());
    }
}
