package com.casemanagement.event.producer;

import com.casemanagement.entity.CaseEvent;
import com.casemanagement.entity.OutboxEvent;
import com.casemanagement.enums.CaseEventType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void publishSendsTopicKeyPayloadAndEventHeaders() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(
                new CaseEvent(eventId, CaseEventType.CASE_CREATED, caseId, Instant.now(),
                        Map.of(), "correlation-1"),
                "{\"event\":\"created\"}");
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, String>) null));
        CaseEventProducer producer = new CaseEventProducer(kafkaTemplate);

        producer.publish(event);

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, String> record = recordCaptor.getValue();
        assertEquals("case-events", record.topic());
        assertEquals(caseId.toString(), record.key());
        assertEquals("{\"event\":\"created\"}", record.value());
        assertEquals(eventId.toString(), new String(
                record.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8));
        assertEquals("correlation-1", new String(
                record.headers().lastHeader("correlationId").value(), StandardCharsets.UTF_8));
    }

    @Test
    void publishWrapsKafkaExecutionFailure() throws Exception {
        OutboxEvent event = new OutboxEvent(
                new CaseEvent(UUID.randomUUID(), CaseEventType.CASE_CREATED, UUID.randomUUID(),
                        Instant.now(), Map.of(), "correlation"),
                "{}");
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new ExecutionException(new IllegalStateException("broker down")));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(failed);

        assertThrows(IllegalStateException.class, () -> new CaseEventProducer(kafkaTemplate).publish(event));
    }
}
