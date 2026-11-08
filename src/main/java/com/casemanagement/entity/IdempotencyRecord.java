package com.casemanagement.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = {@UniqueConstraint(name = "uk_idempotency_user_key", columnNames = {"user_id", "idempotency_key"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IdempotencyRecord(User user, String idempotencyKey, String requestHash, String responseBody, int responseStatus, Instant expiresAt) {
        this.user = user;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.responseBody = responseBody;
        this.responseStatus = responseStatus;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }
}
