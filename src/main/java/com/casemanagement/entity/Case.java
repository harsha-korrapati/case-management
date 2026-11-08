package com.casemanagement.entity;

import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.CaseStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cases",
        indexes = {
                @Index(name = "idx_cases_status", columnList = "status"),
                @Index(name = "idx_cases_priority", columnList = "priority"),
                @Index(name = "idx_cases_deleted", columnList = "deleted"),
                @Index(name = "idx_cases_created_at", columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Case {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Setter
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank
    @Setter
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CaseStatus status = CaseStatus.OPEN;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private CasePriority priority;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_cases_created_by"))
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Case(String title, String description, CasePriority priority, User createdBy) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.createdBy = createdBy;
        this.status = CaseStatus.OPEN;
        this.deleted = false;
    }

    // Used by the service layer for case status updates.
    public void changeStatus(CaseStatus status) {
        this.status = status;
    }

    // Used by the service layer for priority updates.
    public void changePriority(CasePriority priority) {
        this.priority = priority;
    }

    // Used by the service layer for soft deletion.
    public void markDeleted(Instant deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
    }
}

