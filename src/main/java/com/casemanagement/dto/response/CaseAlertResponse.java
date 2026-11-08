package com.casemanagement.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CaseAlertResponse(
        UUID id,
        UUID eventId,
        UUID caseId,
        String reason,
        Instant createdAt
) {
}
