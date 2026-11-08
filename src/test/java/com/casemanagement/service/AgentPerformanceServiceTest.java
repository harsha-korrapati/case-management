package com.casemanagement.service;

import com.casemanagement.dto.response.AgentPerformanceResponse;
import com.casemanagement.repository.AgentPerformanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPerformanceServiceTest {

    @Mock
    private AgentPerformanceRepository repository;

    @InjectMocks
    private AgentPerformanceService service;

    @Test
    void getAgentPerformance_mapsEachProjectionToResponse() {
        UUID agentId = UUID.randomUUID();

        byte[] agentIdBytes = ByteBuffer.allocate(16)
                .putLong(agentId.getMostSignificantBits())
                .putLong(agentId.getLeastSignificantBits())
                .array();

        AgentPerformanceProjection projection =
                mock(AgentPerformanceProjection.class);

        when(projection.getAgentId()).thenReturn(agentIdBytes);
        when(projection.getEmail()).thenReturn("agent@example.com");
        when(projection.getTotalCasesCreated()).thenReturn(7L);
        when(projection.getTotalCasesResolved()).thenReturn(4L);
        when(projection.getAverageResolutionSeconds())
                .thenReturn(new BigDecimal("12.50"));

        when(repository.findAgentPerformance(null, null))
                .thenReturn(List.of(projection));

        List<AgentPerformanceResponse> responses =
                service.getAgentPerformance(null, null);

        assertEquals(1, responses.size());
        assertEquals(agentId, responses.get(0).agentId());
        assertEquals("agent@example.com", responses.get(0).email());
        assertEquals(7L, responses.get(0).totalCasesCreated());
        assertEquals(4L, responses.get(0).totalCasesResolved());
        assertEquals(
                new BigDecimal("12.50"),
                responses.get(0).averageTimeToResolveSeconds()
        );
    }

    @Test
    void getAgentPerformance_rejectsReversedDateRange() {
        Instant from = Instant.parse("2026-02-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        assertThrows(IllegalArgumentException.class, () -> service.getAgentPerformance(from, to));

        verifyNoInteractions(repository);
    }
}
