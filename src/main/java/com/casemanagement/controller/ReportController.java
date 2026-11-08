package com.casemanagement.controller;

import com.casemanagement.dto.response.AgentPerformanceResponse;
import com.casemanagement.dto.response.CaseAlertResponse;
import com.casemanagement.dto.response.CaseSummaryResponse;
import com.casemanagement.service.AgentPerformanceService;
import com.casemanagement.service.CaseAlertReportService;
import com.casemanagement.service.CaseReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final CaseReportService caseReportService;
    private final AgentPerformanceService agentPerformanceService;
    private final CaseAlertReportService caseAlertReportService;

    @GetMapping("/cases/summary")
    public CaseSummaryResponse getCaseSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdAtFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdAtTo
    ) {
        return caseReportService.getCaseSummary(createdAtFrom, createdAtTo);
    }

    @GetMapping("/agents/performance")
    public List<AgentPerformanceResponse> getAgentPerformance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdAtFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdAtTo
    ) {
        return agentPerformanceService.getAgentPerformance(createdAtFrom, createdAtTo);
    }

    @GetMapping("/alerts")
    public Page<CaseAlertResponse> getAlerts(
            @RequestParam(required = false) UUID caseId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdAtFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdAtTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return caseAlertReportService.getAlerts(caseId, reason, createdAtFrom, createdAtTo, pageable);
    }

}
