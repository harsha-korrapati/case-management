package com.casemanagement.dto.response;

import com.casemanagement.entity.CaseComment;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CommentResponse {

    private UUID id;

    private UUID caseId;

    private String comment;

    private UUID createdBy;

    private Instant createdAt;

    public static CommentResponse from(CaseComment commentEntity) {

        return CommentResponse.builder()
                .id(commentEntity.getId())
                .caseId(commentEntity.getCaseId().getId())
                .comment(commentEntity.getComment())
                .createdBy(commentEntity.getCreatedBy().getId())
                .createdAt(commentEntity.getCreatedAt())
                .build();
    }
}