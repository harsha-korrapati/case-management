package com.casemanagement.controller;

import com.casemanagement.dto.response.AgentPerformanceResponse;
import com.casemanagement.dto.response.CaseAlertResponse;
import com.casemanagement.dto.response.CaseSummaryResponse;
import com.casemanagement.service.AgentPerformanceService;
import com.casemanagement.service.CaseAlertReportService;
import com.casemanagement.service.CaseReportService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private CaseReportService caseReportService;
    @Mock
    private AgentPerformanceService agentPerformanceService;
    @Mock
    private CaseAlertReportService caseAlertReportService;
    @InjectMocks
    private ReportController controller;

    @Test
    void summaryPassesOptionalRangeToService() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");
        CaseSummaryResponse summary = new CaseSummaryResponse(java.util.Map.of(), java.util.Map.of());
        when(caseReportService.getCaseSummary(from, to)).thenReturn(summary);

        assertEquals(summary, controller.getCaseSummary(from, to));
        verify(caseReportService).getCaseSummary(from, to);
    }

    @Test
    void agentPerformancePassesRangeToService() {
        Instant from = Instant.now();
        List<AgentPerformanceResponse> report = List.of();
        when(agentPerformanceService.getAgentPerformance(from, null)).thenReturn(report);

        assertEquals(report, controller.getAgentPerformance(from, null));
        verify(agentPerformanceService).getAgentPerformance(from, null);
    }

    @Test
    void alertsPassesFiltersAndPaginationToService() {
        UUID caseId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(1, 10);
        Page<CaseAlertResponse> alerts = new PageImpl<>(List.of(), pageable, 0);
        when(caseAlertReportService.getAlerts(caseId, "reason", null, null, pageable)).thenReturn(alerts);

        assertEquals(alerts, controller.getAlerts(caseId, "reason", null, null, pageable));
        verify(caseAlertReportService).getAlerts(caseId, "reason", null, null, pageable);
    }
}
