package com.casemanagement.service;

import com.casemanagement.entity.CaseEvent;
import com.casemanagement.entity.OutboxEvent;
import com.casemanagement.enums.CaseEventType;
import com.casemanagement.event.producer.CaseEventProducer;
import com.casemanagement.repository.OutboxEventRepository;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository repository;
    @Mock
    private CaseEventProducer producer;
    private ObservationRegistry observationRegistry;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        observationRegistry = ObservationRegistry.create();
        publisher = new OutboxPublisher(repository, producer, observationRegistry);
    }

    @Test
    void publishPendingEventsMarksAndSavesAfterSuccessfulPublish() {
        OutboxEvent event = event();
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));

        publisher.publishPendingEvents();

        verify(producer).publish(event);
        org.junit.jupiter.api.Assertions.assertNotNull(event.getPublishedAt());
        verify(repository).save(event);
    }

    @Test
    void publishPendingEventsDoesNotMarkOrSaveWhenPublishFails() {
        OutboxEvent event = event();
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(event));
        doThrow(new IllegalStateException("broker unavailable")).when(producer).publish(event);

        publisher.publishPendingEvents();

        org.junit.jupiter.api.Assertions.assertNull(event.getPublishedAt());
        verify(repository, never()).save(event);
    }

    @Test
    void publishPendingEventsDoesNothingWhenNoEventsArePending() {
        when(repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(producer, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private OutboxEvent event() {
        return new OutboxEvent(
                new CaseEvent(UUID.randomUUID(), CaseEventType.CASE_CREATED, UUID.randomUUID(),
                        Instant.now(), Map.of(), "test-correlation"),
                "{}");
    }
}
