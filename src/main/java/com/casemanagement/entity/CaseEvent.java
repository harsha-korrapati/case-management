package com.casemanagement.entity;

import com.casemanagement.enums.CaseEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CaseEvent(
        UUID eventId,
        CaseEventType eventType,
        UUID caseId,
        Instant timestamp,
        Map<String, Object> payload,
        String correlationId
) {
}
