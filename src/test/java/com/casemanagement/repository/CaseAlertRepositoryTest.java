package com.casemanagement.repository;

import com.casemanagement.entity.CaseAlert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseAlertRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private CaseAlertRepository caseAlertRepository;

    @Test
    void checksWhetherEventHasAlreadyProducedAnAlert() {
        UUID eventId = UUID.randomUUID();
        caseAlertRepository.saveAndFlush(
                new CaseAlert(eventId, UUID.randomUUID(), "CRITICAL_PRIORITY"));

        assertTrue(caseAlertRepository.existsByEventId(eventId));
        assertFalse(caseAlertRepository.existsByEventId(UUID.randomUUID()));
    }
}
