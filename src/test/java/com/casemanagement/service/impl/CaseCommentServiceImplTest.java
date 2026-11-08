package com.casemanagement.service.impl;

import com.casemanagement.dto.request.CreateCommentRequest;
import com.casemanagement.dto.response.CommentResponse;
import com.casemanagement.entity.Case;
import com.casemanagement.entity.CaseComment;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.UserRole;
import com.casemanagement.exception.ForbiddenException;
import com.casemanagement.exception.ResourceNotFoundException;
import com.casemanagement.repository.CaseCommentRepository;
import com.casemanagement.repository.CaseRepository;
import com.casemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseCommentServiceImplTest {

    @Mock
    private CaseCommentRepository commentRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CaseCommentServiceImpl service;

    private UUID caseId;
    private UUID userId;
    private Case activeCase;
    private User agent;

    @BeforeEach
    void setUp() {
        caseId = UUID.randomUUID();
        userId = UUID.randomUUID();
        agent = new User("agent@example.com", "hashed", UserRole.AGENT);
        ReflectionTestUtils.setField(agent, "id", userId);
        activeCase = new Case("Test", "Description", CasePriority.HIGH, agent);
        ReflectionTestUtils.setField(activeCase, "id", caseId);
    }

    @Test
    void createCommentSavesCommentForActiveCaseAndAgent() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setComment("A useful update");
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(activeCase));
        when(userRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(commentRepository.save(any(CaseComment.class))).thenAnswer(invocation -> {
            CaseComment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
            return comment;
        });

        CommentResponse response = service.createComment(caseId, request, userId);

        assertEquals("A useful update", response.getComment());
        assertEquals(caseId, response.getCaseId());
        assertEquals(userId, response.getCreatedBy());
        verify(commentRepository).save(any(CaseComment.class));
    }

    @Test
    void createCommentRejectsDeletedCase() {
        activeCase.markDeleted(java.time.Instant.now());
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(activeCase));

        assertThrows(ResourceNotFoundException.class, () ->
                service.createComment(caseId, new CreateCommentRequest(), userId));

        verifyNoInteractions(userRepository, commentRepository);
    }

    @Test
    void createCommentRejectsUserWithoutCommentPermission() {
        User viewer = new User("viewer@example.com", "hashed", UserRole.VIEWER);
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(activeCase));
        when(userRepository.findById(userId)).thenReturn(Optional.of(viewer));

        assertThrows(ForbiddenException.class, () ->
                service.createComment(caseId, new CreateCommentRequest(), userId));

        verifyNoInteractions(commentRepository);
    }

    @Test
    void getCommentsValidatesCaseAndMapsPage() {
        PageRequest pageable = PageRequest.of(0, 5);
        CaseComment comment = new CaseComment(activeCase, "Stored comment", agent);
        ReflectionTestUtils.setField(comment, "id", UUID.randomUUID());
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(activeCase));
        when(commentRepository.findByCaseId_IdAndCaseId_DeletedFalse(caseId, pageable))
                .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));

        Page<CommentResponse> response = service.getComments(caseId, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Stored comment", response.getContent().get(0).getComment());
        verify(commentRepository).findByCaseId_IdAndCaseId_DeletedFalse(caseId, pageable);
    }
}
