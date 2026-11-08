package com.casemanagement.controller;

import com.casemanagement.dto.request.CreateCommentRequest;
import com.casemanagement.dto.response.CommentResponse;
import com.casemanagement.security.AuthenticatedUser;
import com.casemanagement.service.CaseCommentService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.UUID;

@RestController
@RequestMapping("/cases/{caseId}/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CaseCommentController {

    private final CaseCommentService caseCommentService;

    // Create a comment
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID caseId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {

        CommentResponse response = caseCommentService.createComment(caseId, request, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get comments with pagination
    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable UUID caseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<CommentResponse> comments = caseCommentService.getComments(caseId, pageable);

        return ResponseEntity.ok(comments);
    }
}

