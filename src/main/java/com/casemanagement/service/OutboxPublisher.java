package com.casemanagement.service;

import com.casemanagement.entity.OutboxEvent;
import com.casemanagement.repository.OutboxEventRepository;
import com.casemanagement.event.producer.CaseEventProducer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final CaseEventProducer caseEventProducer;
    private final ObservationRegistry observationRegistry;

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                Observation.createNotStarted("outbox.publish", observationRegistry)
                        .lowCardinalityKeyValue("event.type", event.getEventType().name())
                        .observe(() -> {

                            // Publish and wait for Kafka acknowledgement.
                            caseEventProducer.publish(event);

                            // Mark as published only after successful acknowledgement.
                            event.markPublished();

                            outboxEventRepository.save(event);

                            log.info(
                                    "Outbox event published successfully. eventId={}, caseId={}",
                                    event.getId(),
                                    event.getCaseId()
                            );
                        });

            } catch (Exception exception) {

                log.error(
                        "Failed to publish outbox event. eventId={}",
                        event.getId(),
                        exception
                );
            }
        }
    }
}