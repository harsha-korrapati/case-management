package com.casemanagement.service.impl;

import com.casemanagement.dto.request.CreateCommentRequest;
import com.casemanagement.dto.response.CommentResponse;
import com.casemanagement.entity.Case;
import com.casemanagement.entity.CaseComment;
import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import com.casemanagement.exception.ForbiddenException;
import com.casemanagement.exception.ResourceNotFoundException;
import com.casemanagement.repository.CaseCommentRepository;
import com.casemanagement.repository.CaseRepository;
import com.casemanagement.repository.UserRepository;
import com.casemanagement.service.CaseCommentService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CaseCommentServiceImpl implements CaseCommentService {

    private final CaseCommentRepository caseCommentRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Override
    public CommentResponse createComment(UUID caseId, CreateCommentRequest request, UUID currentUserId) {

        // 1. Retrieve the case and verify that it is active.
        Case caseEntity = getActiveCase(caseId);

        // 2. Retrieve the authenticated user.
        User user = getUser(currentUserId);

        // 3. Check whether the user can create comments.
        if (!hasCommentPermission(user)) {
            throw new ForbiddenException("You are not authorized to create comments");
        }

        // 4. Create the comment.
        CaseComment caseComment = new CaseComment(caseEntity, request.getComment(), user);

        // 5. Save and return the response.
        CaseComment savedComment = caseCommentRepository.save(caseComment);

        return CommentResponse.from(savedComment);
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(UUID caseId, Pageable pageable) {

        // Verify that the case exists and is not deleted.
        getActiveCase(caseId);

        // Retrieve comments with pagination.
        return caseCommentRepository
                .findByCaseId_IdAndCaseId_DeletedFalse(caseId, pageable)
                .map(CommentResponse::from);
    }

    private Case getActiveCase(UUID caseId) {

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        if (caseEntity.isDeleted()) {
            throw new ResourceNotFoundException("Case not found");
        }

        return caseEntity;
    }

    private User getUser(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private boolean hasCommentPermission(User user) {

        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.AGENT;
    }
}
