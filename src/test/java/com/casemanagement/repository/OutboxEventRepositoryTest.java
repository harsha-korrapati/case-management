package com.casemanagement.repository;

import com.casemanagement.entity.CaseEvent;
import com.casemanagement.entity.OutboxEvent;
import com.casemanagement.enums.CaseEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxEventRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void returnsOnlyUnpublishedEventsInCreationOrder() {
        OutboxEvent pending = event();
        OutboxEvent published = event();
        published.markPublished();
        outboxEventRepository.saveAndFlush(pending);
        outboxEventRepository.saveAndFlush(published);

        List<OutboxEvent> result =
                outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        assertEquals(1, result.size());
        assertEquals(pending.getId(), result.get(0).getId());
    }

    private OutboxEvent event() {
        return new OutboxEvent(
                new CaseEvent(UUID.randomUUID(), CaseEventType.CASE_CREATED, UUID.randomUUID(),
                        Instant.now(), Map.of(), "repository-test"),
                "{}");
    }
}
