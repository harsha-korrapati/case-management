package com.casemanagement.dto.response;

import com.casemanagement.entity.Case;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.CaseStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseResponse {

    private UUID id;

    private String title;

    private String description;

    private CaseStatus status;

    private CasePriority priority;

    private UUID createdBy;

    private Instant createdAt;

    private Instant updatedAt;

    public static CaseResponse from(Case caseEntity) {

        return CaseResponse.builder()
                .id(caseEntity.getId())
                .title(caseEntity.getTitle())
                .description(caseEntity.getDescription())
                .status(caseEntity.getStatus())
                .priority(caseEntity.getPriority())
                .createdBy(caseEntity.getCreatedBy().getId())
                .createdAt(caseEntity.getCreatedAt())
                .updatedAt(caseEntity.getUpdatedAt())
                .build();
    }
}
