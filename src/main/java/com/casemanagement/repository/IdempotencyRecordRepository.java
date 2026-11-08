package com.casemanagement.repository;

import com.casemanagement.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByUser_IdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
