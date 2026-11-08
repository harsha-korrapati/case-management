package com.casemanagement.dto.response;

import java.util.Map;

public record CaseSummaryResponse(
        Map<String, Long> statusCounts,
        Map<String, Long> priorityCounts
) {
}
