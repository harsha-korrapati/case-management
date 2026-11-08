package com.casemanagement.controller;

import com.casemanagement.dto.request.CreateCommentRequest;
import com.casemanagement.dto.response.CommentResponse;
import com.casemanagement.security.AuthenticatedUser;
import com.casemanagement.service.CaseCommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseCommentControllerTest {

    @Mock
    private CaseCommentService caseCommentService;

    @Mock
    private AuthenticatedUser currentUser;

    @InjectMocks
    private CaseCommentController controller;

    @Test
    void createCommentReturnsCreatedResponseAndAuthenticatedUserId() {
        UUID caseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest();
        CommentResponse comment = CommentResponse.builder().comment("hello").build();
        when(currentUser.getId()).thenReturn(userId);
        when(caseCommentService.createComment(caseId, request, userId)).thenReturn(comment);

        var response = controller.createComment(caseId, request, currentUser);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(comment, response.getBody());
        verify(caseCommentService).createComment(caseId, request, userId);
    }

    @Test
    void getCommentsBuildsDescendingCreatedAtPage() {
        UUID caseId = UUID.randomUUID();
        Page<CommentResponse> comments = new PageImpl<>(List.of());
        when(caseCommentService.getComments(org.mockito.ArgumentMatchers.eq(caseId), any()))
                .thenReturn(comments);

        var response = controller.getComments(caseId, 1, 25);

        assertEquals(comments, response.getBody());
        verify(caseCommentService).getComments(org.mockito.ArgumentMatchers.eq(caseId),
                org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.equals(PageRequest.of(1, 25,
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))));
    }

    @Test
    void getCommentsRejectsOutOfRangePagination() {
        UUID caseId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> controller.getComments(caseId, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> controller.getComments(caseId, 0, 101));

        verify(caseCommentService, never()).getComments(any(), any());
    }
}
