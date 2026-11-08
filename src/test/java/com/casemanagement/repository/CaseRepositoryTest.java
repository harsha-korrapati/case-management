package com.casemanagement.repository;

import com.casemanagement.entity.Case;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.UserRole;
import com.casemanagement.specification.CaseSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndRetrievesCaseById() {
        User user = saveAgent();
        Case created = caseRepository.saveAndFlush(
                new Case("Repository test", "Description", CasePriority.HIGH, user));

        Case retrieved = caseRepository.findById(created.getId()).orElseThrow();

        assertEquals(created.getId(), retrieved.getId());
        assertEquals("Repository test", retrieved.getTitle());
        assertEquals(user.getId(), retrieved.getCreatedBy().getId());
    }

    @Test
    void supportsSpecificationsForActiveCases() {
        User user = saveAgent();
        Case activeCase = caseRepository.saveAndFlush(
                new Case("Active case", "Description", CasePriority.LOW, user));
        Case deletedCase = new Case("Deleted case", "Description", CasePriority.HIGH, user);
        deletedCase.markDeleted(Instant.now());
        caseRepository.saveAndFlush(deletedCase);

        var activeCases = caseRepository.findAll(CaseSpecification.notDeleted());

        assertTrue(activeCases.stream().anyMatch(entity -> entity.getId().equals(activeCase.getId())));
        assertFalse(activeCases.stream().anyMatch(entity -> entity.getId().equals(deletedCase.getId())));
    }

    private User saveAgent() {
        return userRepository.saveAndFlush(
                new User(UUID.randomUUID() + "@example.com", "hashed", UserRole.AGENT));
    }
}
