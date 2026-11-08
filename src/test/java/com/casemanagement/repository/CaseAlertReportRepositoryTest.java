package com.casemanagement.repository;

import com.casemanagement.entity.CaseAlert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseAlertReportRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private CaseAlertReportRepository repository;

    @Test
    void searchesAlertsByCaseReasonAndCreatedAtRange() {
        UUID caseId = UUID.randomUUID();
        repository.saveAndFlush(new CaseAlert(UUID.randomUUID(), caseId, "CRITICAL_PRIORITY"));
        repository.saveAndFlush(new CaseAlert(UUID.randomUUID(), caseId, "STATUS_RESOLVED"));
        repository.saveAndFlush(new CaseAlert(UUID.randomUUID(), UUID.randomUUID(), "CRITICAL_PRIORITY"));
        Instant from = Instant.now().minusSeconds(10);
        Instant to = Instant.now().plusSeconds(10);

        Page<CaseAlert> matching = repository.searchAlerts(
                caseId, "CRITICAL_PRIORITY", from, to, PageRequest.of(0, 10));
        Page<CaseAlert> unfiltered = repository.searchAlerts(
                null, null, null, null, PageRequest.of(0, 10));

        assertEquals(1, matching.getTotalElements());
        assertEquals("CRITICAL_PRIORITY", matching.getContent().get(0).getReason());
        assertEquals(3, unfiltered.getTotalElements());
    }
}
