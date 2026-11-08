package com.casemanagement.event.producer;

import com.casemanagement.entity.OutboxEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
//import java.util.concurrent


@Component
public class CaseEventProducer {

    private static final String TOPIC = "case-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CaseEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OutboxEvent event) {

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        TOPIC,
                        event.getCaseId().toString(),
                        event.getPayload()
                );

        record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));

        record.headers().add("correlationId", event.getCorrelationId().getBytes(StandardCharsets.UTF_8));

        try {
            // Wait for Kafka broker acknowledgment.
            kafkaTemplate.send(record).get();

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka event publishing was interrupted", exception);

        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to publish case event to Kafka", exception);
        }
    }
}
