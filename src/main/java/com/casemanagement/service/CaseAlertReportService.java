package com.casemanagement.service;

import com.casemanagement.entity.CaseAlert;

import com.casemanagement.dto.response.CaseAlertResponse;
import com.casemanagement.repository.CaseAlertReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseAlertReportService {

    private final CaseAlertReportRepository caseAlertReportRepository;

    public Page<CaseAlertResponse> getAlerts(UUID caseId, String reason, Instant createdAtFrom, Instant createdAtTo, Pageable pageable) {
        validateDateRange(createdAtFrom, createdAtTo);

        if (reason != null) {
            reason = reason.trim();

            if (reason.isEmpty()) {
                reason = null;
            }
        }

        Page<CaseAlert> alerts = caseAlertReportRepository.searchAlerts(caseId, reason, createdAtFrom, createdAtTo, pageable);

        return alerts.map(this::toResponse);
    }

    private CaseAlertResponse toResponse(CaseAlert alert) {
        return new CaseAlertResponse(
                alert.getId(),
                alert.getEventId(),
                alert.getCaseId(),
                alert.getReason(),
                alert.getCreatedAt()
        );
    }

    private void validateDateRange(Instant createdAtFrom, Instant createdAtTo) {
        if (createdAtFrom != null && createdAtTo != null && createdAtFrom.isAfter(createdAtTo)) {
            throw new IllegalArgumentException("createdAtFrom must be before or equal to createdAtTo");
        }
    }
}
