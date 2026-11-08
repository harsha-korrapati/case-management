package com.casemanagement.repository;

import com.casemanagement.entity.Case;
import com.casemanagement.entity.CaseComment;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseCommentRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private CaseCommentRepository caseCommentRepository;
    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void findsCommentsForActiveCaseAndPagesResults() {
        User user = userRepository.saveAndFlush(
                new User(UUID.randomUUID() + "@example.com", "hashed", UserRole.AGENT));
        Case activeCase = caseRepository.saveAndFlush(
                new Case("Active", "Description", CasePriority.HIGH, user));
        Case deletedCase = new Case("Deleted", "Description", CasePriority.LOW, user);
        deletedCase.markDeleted(Instant.now());
        caseRepository.saveAndFlush(deletedCase);
        caseCommentRepository.saveAndFlush(new CaseComment(activeCase, "First comment", user));
        caseCommentRepository.saveAndFlush(new CaseComment(activeCase, "Second comment", user));
        caseCommentRepository.saveAndFlush(new CaseComment(deletedCase, "Hidden comment", user));

        Page<CaseComment> comments =
                caseCommentRepository.findByCaseId_IdAndCaseId_DeletedFalse(
                        activeCase.getId(), PageRequest.of(0, 1));

        assertEquals(2, comments.getTotalElements());
        assertEquals(1, comments.getNumberOfElements());
        assertEquals(activeCase.getId(), comments.getContent().get(0).getCaseId().getId());
    }
}
