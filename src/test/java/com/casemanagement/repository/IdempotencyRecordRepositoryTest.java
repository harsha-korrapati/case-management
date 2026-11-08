package com.casemanagement.repository;

import com.casemanagement.entity.IdempotencyRecord;
import com.casemanagement.entity.User;
import com.casemanagement.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyRecordRepositoryTest extends AbstractMySqlRepositorySupport {

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void findsIdempotencyRecordByUserIdAndKey() {
        User user = userRepository.saveAndFlush(
                new User(UUID.randomUUID() + "@example.com", "hashed", UserRole.AGENT));
        String key = UUID.randomUUID().toString();
        idempotencyRecordRepository.saveAndFlush(new IdempotencyRecord(
                user, key, "request-hash", "{\"id\":\"result\"}", 201,
                Instant.now().plusSeconds(3600)));

        IdempotencyRecord found = idempotencyRecordRepository
                .findByUser_IdAndIdempotencyKey(user.getId(), key)
                .orElseThrow();

        assertEquals("request-hash", found.getRequestHash());
        assertEquals("{\"id\":\"result\"}", found.getResponseBody());
        assertEquals(201, found.getResponseStatus());
        assertTrue(idempotencyRecordRepository
                .findByUser_IdAndIdempotencyKey(UUID.randomUUID(), key).isEmpty());
        assertTrue(idempotencyRecordRepository
                .findByUser_IdAndIdempotencyKey(user.getId(), "missing-key").isEmpty());
    }
}
