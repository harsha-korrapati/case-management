package com.casemanagement.event.consumer;

import com.casemanagement.config.ApplicationMetrics;
import com.casemanagement.entity.CaseEvent;
import com.casemanagement.enums.CaseEventType;
import com.casemanagement.service.CaseAlertService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseEventConsumerTest {

    @Mock
    private JsonMapper jsonMapper;
    @Mock
    private CaseAlertService caseAlertService;
    @Mock
    private ApplicationMetrics metrics;
    @Mock
    private Acknowledgment acknowledgment;
    @InjectMocks
    private CaseEventConsumer consumer;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void consumeProcessesEventCountsAndAcknowledgesOnSuccess() throws Exception {
        CaseEvent event = event();
        when(jsonMapper.readValue("message", CaseEvent.class)).thenReturn(event);

        consumer.consume("message", acknowledgment);

        verify(caseAlertService).processEvent(event);
        verify(metrics).incrementKafkaEventsConsumed();
        verify(acknowledgment).acknowledge();
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void consumeDoesNotAcknowledgeAndCountsFailureWhenProcessingThrows() throws Exception {
        CaseEvent event = event();
        when(jsonMapper.readValue("message", CaseEvent.class)).thenReturn(event);
        doThrow(new IllegalStateException("processing failed")).when(caseAlertService).processEvent(event);

        assertThrows(IllegalStateException.class, () -> consumer.consume("message", acknowledgment));

        verify(metrics).incrementKafkaEventsFailed();
        verify(metrics, never()).incrementKafkaEventsConsumed();
        verify(acknowledgment, never()).acknowledge();
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void consumeRejectsMalformedJsonBeforeAcknowledging() throws Exception {
        JsonMapper realMapper = new JsonMapper();
        CaseEventConsumer consumerWithRealMapper =
                new CaseEventConsumer(realMapper, caseAlertService, metrics);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> consumerWithRealMapper.consume("{invalid", acknowledgment));

        assertEquals("Unable to deserialize case event", exception.getMessage());
        verify(acknowledgment, never()).acknowledge();
        verify(caseAlertService, never()).processEvent(org.mockito.ArgumentMatchers.any());
    }

    private CaseEvent event() {
        return new CaseEvent(UUID.randomUUID(), CaseEventType.CASE_CREATED, UUID.randomUUID(),
                Instant.now(), Map.of(), "correlation-test");
    }
}
