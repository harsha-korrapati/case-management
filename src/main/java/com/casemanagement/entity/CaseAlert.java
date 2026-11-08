package com.casemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseAlert {

    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CaseAlert(UUID eventId, UUID caseId, String reason) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.caseId = caseId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
}
