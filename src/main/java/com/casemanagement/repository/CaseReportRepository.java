package com.casemanagement.repository;

import com.casemanagement.entity.Case;
import com.casemanagement.service.CaseCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CaseReportRepository
        extends JpaRepository<Case, UUID> {

    @Query(value = """
            SELECT
                status AS category,
                COUNT(*) AS total
            FROM cases
            WHERE deleted = false
              AND (:createdAtFrom IS NULL OR created_at >= :createdAtFrom)
              AND (:createdAtTo IS NULL OR created_at <= :createdAtTo)
            GROUP BY status
            ORDER BY status
            """, nativeQuery = true)
    List<CaseCountProjection> countCasesByStatus(
            @Param("createdAtFrom") Instant createdAtFrom,
            @Param("createdAtTo") Instant createdAtTo
    );

    @Query(value = """
            SELECT
                priority AS category,
                COUNT(*) AS total
            FROM cases
            WHERE deleted = false
              AND (:createdAtFrom IS NULL OR created_at >= :createdAtFrom)
              AND (:createdAtTo IS NULL OR created_at <= :createdAtTo)
            GROUP BY priority
            ORDER BY priority
            """, nativeQuery = true)
    List<CaseCountProjection> countCasesByPriority(
            @Param("createdAtFrom") Instant createdAtFrom,
            @Param("createdAtTo") Instant createdAtTo
    );
}
