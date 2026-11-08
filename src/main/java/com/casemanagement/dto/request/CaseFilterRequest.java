package com.casemanagement.dto.request;

import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.CaseStatus;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CaseFilterRequest {

    private CaseStatus status;

    private CasePriority priority;

    private UUID createdBy;

    private LocalDateTime createdAtFrom;

    private LocalDateTime createdAtTo;

    @AssertTrue(message = "createdAtFrom must be before or equal to createdAtTo")
    public boolean isValidDateRange() {

        if (createdAtFrom == null || createdAtTo == null) {
            return true;
        }

        return !createdAtFrom.isAfter(createdAtTo);
    }
}
