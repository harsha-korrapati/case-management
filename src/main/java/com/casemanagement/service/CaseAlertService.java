package com.casemanagement.service;

import com.casemanagement.config.ApplicationMetrics;
import com.casemanagement.entity.CaseAlert;
import com.casemanagement.entity.CaseEvent;
import com.casemanagement.repository.CaseAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CaseAlertService {

    private final CaseAlertRepository caseAlertRepository;
    private final ApplicationMetrics applicationMetrics;

    @Transactional
    public void processEvent(CaseEvent event) {

        // 1. Check whether this event was already processed.
        if (caseAlertRepository.existsByEventId(event.eventId())) {
            return;
        }

        Map<String, Object> payload = event.payload();

        // 2. Evaluate alert conditions.
        boolean isCritical = "CRITICAL".equals(payload.get("priority"));

        boolean isResolved = event.eventType().name().equals("CASE_STATUS_CHANGED") && "RESOLVED".equals(payload.get("status"));

        // 3. No alert required.
        if (!isCritical && !isResolved) {
            return;
        }

        // 4. Determine alert reason.
        String reason;

        if (isCritical && isResolved) {
            reason = "CRITICAL_PRIORITY_AND_RESOLVED";
        } else if (isCritical) {
            reason = "CRITICAL_PRIORITY";
        } else {
            reason = "STATUS_RESOLVED";
        }

        // 5. Create and persist alert.
        CaseAlert alert = new CaseAlert(event.eventId(), event.caseId(), reason);

        caseAlertRepository.save(alert);

        applicationMetrics.incrementAlertsCreated();
    }
}
