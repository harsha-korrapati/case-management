package com.casemanagement.service;

import com.casemanagement.dto.response.CaseSummaryResponse;
import com.casemanagement.repository.CaseReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseReportServiceTest {

    @Mock
    private CaseReportRepository repository;

    @Mock
    private CaseCountProjection statusCount;

    @Mock
    private CaseCountProjection priorityCount;

    @InjectMocks
    private CaseReportService service;

    @Test
    void getCaseSummary_mapsRepositoryCounts() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        when(statusCount.getCategory()).thenReturn("OPEN");
        when(statusCount.getTotal()).thenReturn(3L);
        when(priorityCount.getCategory()).thenReturn("HIGH");
        when(priorityCount.getTotal()).thenReturn(2L);
        when(repository.countCasesByStatus(from, to)).thenReturn(List.of(statusCount));
        when(repository.countCasesByPriority(from, to)).thenReturn(List.of(priorityCount));

        CaseSummaryResponse response = service.getCaseSummary(from, to);

        assertEquals(3L, response.statusCounts().get("OPEN"));
        assertEquals(2L, response.priorityCounts().get("HIGH"));
        verify(repository).countCasesByStatus(from, to);
        verify(repository).countCasesByPriority(from, to);
    }

    @Test
    void getCaseSummary_rejectsReversedDateRangeBeforeRepositoryAccess() {
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> service.getCaseSummary(from, to));

        verifyNoInteractions(repository);
    }
}
