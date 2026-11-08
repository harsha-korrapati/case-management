package com.casemanagement.service;

import com.casemanagement.dto.response.CaseAlertResponse;
import com.casemanagement.entity.CaseAlert;
import com.casemanagement.repository.CaseAlertReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseAlertReportServiceTest {

    @Mock
    private CaseAlertReportRepository repository;

    @InjectMocks
    private CaseAlertReportService service;

    @Test
    void getAlerts_trimsReasonAndMapsAlertPage() {
        UUID caseId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-01T10:00:00Z");
        CaseAlert alert = new CaseAlert(eventId, caseId, "STATUS_RESOLVED");
        PageRequest pageable = PageRequest.of(0, 10);
        Page<CaseAlert> alertPage = new PageImpl<>(List.of(alert), pageable, 1);
        when(repository.searchAlerts(caseId, "STATUS_RESOLVED", null, null, pageable))
                .thenReturn(alertPage);

        Page<CaseAlertResponse> response =
                service.getAlerts(caseId, " STATUS_RESOLVED ", null, null, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(eventId, response.getContent().get(0).eventId());
        assertEquals(caseId, response.getContent().get(0).caseId());
        assertEquals("STATUS_RESOLVED", response.getContent().get(0).reason());
        verify(repository).searchAlerts(caseId, "STATUS_RESOLVED", null, null, pageable);
    }

    @Test
    void getAlerts_convertsBlankReasonToNull() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(repository.searchAlerts(null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        assertEquals(0, service.getAlerts(null, "   ", null, null, pageable).getTotalElements());

        verify(repository).searchAlerts(null, null, null, null, pageable);
    }

    @Test
    void getAlerts_rejectsReversedDateRangeBeforeRepositoryAccess() {
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> service.getAlerts(null, null, from, to, PageRequest.of(0, 10)));

        verifyNoInteractions(repository);
    }
}
