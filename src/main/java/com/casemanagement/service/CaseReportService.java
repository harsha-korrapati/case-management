package com.casemanagement.service;

import com.casemanagement.dto.response.CaseSummaryResponse;
import com.casemanagement.service.CaseCountProjection;
import com.casemanagement.repository.CaseReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseReportService {

    private final CaseReportRepository caseReportRepository;

    public CaseSummaryResponse getCaseSummary(Instant createdAtFrom, Instant createdAtTo) {
        validateDateRange(createdAtFrom, createdAtTo);

        Map<String, Long> statusCounts =
                convertToMap(
                        caseReportRepository.countCasesByStatus(
                                createdAtFrom,
                                createdAtTo
                        )
                );

        Map<String, Long> priorityCounts =
                convertToMap(
                        caseReportRepository.countCasesByPriority(
                                createdAtFrom,
                                createdAtTo
                        )
                );

        return new CaseSummaryResponse(statusCounts, priorityCounts);
    }

    private Map<String, Long> convertToMap(java.util.List<CaseCountProjection> projections) {
        Map<String, Long> result = new LinkedHashMap<>();

        projections.forEach(projection ->
                result.put(
                        projection.getCategory(),
                        projection.getTotal()
                )
        );

        return result;
    }

    private void validateDateRange(Instant createdAtFrom, Instant createdAtTo) {
        if (createdAtFrom != null && createdAtTo != null && createdAtFrom.isAfter(createdAtTo)) {
            throw new IllegalArgumentException("createdAtFrom must be before or equal to createdAtTo");
        }
    }
}
