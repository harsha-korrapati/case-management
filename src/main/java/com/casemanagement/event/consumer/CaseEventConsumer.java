package com.casemanagement.event.consumer;

import com.casemanagement.config.ApplicationMetrics;
import com.casemanagement.entity.CaseEvent;
import com.casemanagement.service.CaseAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaseEventConsumer {

    private final JsonMapper jsonMapper;
    private final CaseAlertService caseAlertService;
    private final ApplicationMetrics applicationMetrics;

    @KafkaListener(
            topics = "case-events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message, Acknowledgment acknowledgment) {
        CaseEvent event;

        // Step 1: Deserialize the incoming JSON.
        try {
            event = jsonMapper.readValue(message, CaseEvent.class);
            log.info(
                    "Processing case event: eventId={}, caseId={}, eventType={}",
                    event.eventId(),
                    event.caseId(),
                    event.eventType()
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to deserialize case event", exception);
        }

        // Step 2: Add correlation ID to logging context.
        MDC.put("correlationId", event.correlationId());

        try {
            // Step 3: Process the event.
            // The transactional service completes before returning.
            caseAlertService.processEvent(event);

            // Count only successfully processed events.
            applicationMetrics.incrementKafkaEventsConsumed();

            // Step 4: Acknowledge only after successful processing.
            acknowledgment.acknowledge();

            log.info(
                    "Case event processed successfully. eventId={}, caseId={}, eventType={}",
                    event.eventId(),
                    event.caseId(),
                    event.eventType()
            );

        } catch (Exception exception) {
            log.error(
                    "Failed to process case event: eventId={}",
                    event.eventId(),
                    exception
            );

            // Count failed processing attempts.
            applicationMetrics.incrementKafkaEventsFailed();

            // Rethrow so Kafka's configured retry/DLQ mechanism handles it.
            throw exception;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
