package com.casemanagement.repository;

import com.casemanagement.entity.CaseAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface CaseAlertReportRepository
        extends JpaRepository<CaseAlert, UUID> {

    @Query("""
            SELECT a
            FROM CaseAlert a
            WHERE (:caseId IS NULL OR a.caseId = :caseId)
              AND (:reason IS NULL OR a.reason = :reason)
              AND (:createdAtFrom IS NULL OR a.createdAt >= :createdAtFrom)
              AND (:createdAtTo IS NULL OR a.createdAt <= :createdAtTo)
            """)
    Page<CaseAlert> searchAlerts(
            @Param("caseId") UUID caseId,
            @Param("reason") String reason,
            @Param("createdAtFrom") Instant createdAtFrom,
            @Param("createdAtTo") Instant createdAtTo,
            Pageable pageable
    );
}
