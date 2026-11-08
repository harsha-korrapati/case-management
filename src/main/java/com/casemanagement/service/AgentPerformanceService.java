package com.casemanagement.service;

import com.casemanagement.dto.response.AgentPerformanceResponse;
import com.casemanagement.service.AgentPerformanceProjection;
import com.casemanagement.repository.AgentPerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentPerformanceService {

    private final AgentPerformanceRepository agentPerformanceRepository;

    public List<AgentPerformanceResponse> getAgentPerformance(
            Instant createdAtFrom,
            Instant createdAtTo
    ) {
        validateDateRange(createdAtFrom, createdAtTo);

        return agentPerformanceRepository.findAgentPerformance(
                        createdAtFrom,
                        createdAtTo
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AgentPerformanceResponse toResponse(
            AgentPerformanceProjection projection
    ) {
        return new AgentPerformanceResponse(
                toUuid(projection.getAgentId()),
                projection.getEmail(),
                projection.getTotalCasesCreated(),
                projection.getTotalCasesResolved(),
                projection.getAverageResolutionSeconds()
        );
    }

    private void validateDateRange(
            Instant createdAtFrom,
            Instant createdAtTo
    ) {
        if (createdAtFrom != null
                && createdAtTo != null
                && createdAtFrom.isAfter(createdAtTo)) {
            throw new IllegalArgumentException(
                    "createdAtFrom must be before or equal to createdAtTo"
            );
        }
    }

    private UUID toUuid(byte[] bytes) {

        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException(
                    "Invalid binary UUID received from database"
            );
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        long mostSignificantBits = byteBuffer.getLong();
        long leastSignificantBits = byteBuffer.getLong();

        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
