package com.casemanagement.service;

import com.casemanagement.config.ApplicationMetrics;
import com.casemanagement.entity.CaseEvent;
import com.casemanagement.enums.CaseEventType;
import com.casemanagement.repository.CaseAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseAlertServiceTest {

    @Mock
    private CaseAlertRepository repository;

    @Mock
    private ApplicationMetrics metrics;

    @InjectMocks
    private CaseAlertService service;

    @Test
    void processEvent_createsAlertForCriticalPriority() {
        CaseEvent event = event(Map.of("priority", "CRITICAL", "status", "OPEN"),
                CaseEventType.CASE_CREATED);
        when(repository.existsByEventId(event.eventId())).thenReturn(false);

        service.processEvent(event);

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(alert ->
                alert.getEventId().equals(event.eventId())
                        && alert.getCaseId().equals(event.caseId())
                        && alert.getReason().equals("CRITICAL_PRIORITY")));
        verify(metrics).incrementAlertsCreated();
    }

    @Test
    void processEvent_createsCombinedReasonForCriticalResolvedEvent() {
        CaseEvent event = event(Map.of("priority", "CRITICAL", "status", "RESOLVED"),
                CaseEventType.CASE_STATUS_CHANGED);
        when(repository.existsByEventId(event.eventId())).thenReturn(false);

        service.processEvent(event);

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(alert ->
                alert.getReason().equals("CRITICAL_PRIORITY_AND_RESOLVED")));
        verify(metrics).incrementAlertsCreated();
    }

    @Test
    void processEvent_ignoresAlreadyProcessedEvents() {
        CaseEvent event = event(Map.of("priority", "CRITICAL"), CaseEventType.CASE_CREATED);
        when(repository.existsByEventId(event.eventId())).thenReturn(true);

        service.processEvent(event);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(metrics, never()).incrementAlertsCreated();
    }

    @Test
    void processEvent_ignoresNonAlertingEvent() {
        CaseEvent event = event(Map.of("priority", "LOW", "status", "OPEN"),
                CaseEventType.CASE_CREATED);
        when(repository.existsByEventId(event.eventId())).thenReturn(false);

        service.processEvent(event);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(metrics, never()).incrementAlertsCreated();
    }

    private CaseEvent event(Map<String, Object> payload, CaseEventType type) {
        return new CaseEvent(UUID.randomUUID(), type, UUID.randomUUID(), Instant.now(), payload, "test");
    }
}
