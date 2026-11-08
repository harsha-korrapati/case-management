package com.casemanagement.service;

import com.casemanagement.dto.request.CreateCommentRequest;
import com.casemanagement.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CaseCommentService {

    CommentResponse createComment(UUID caseId, CreateCommentRequest request, UUID currentUserId);

    Page<CommentResponse> getComments(UUID caseId, Pageable pageable);
}
