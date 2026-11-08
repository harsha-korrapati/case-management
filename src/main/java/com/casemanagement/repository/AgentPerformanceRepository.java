package com.casemanagement.repository;

import com.casemanagement.entity.User;
import com.casemanagement.service.AgentPerformanceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgentPerformanceRepository
        extends JpaRepository<User, UUID> {

    @Query(value = """
            WITH first_resolution AS (
                SELECT
                    oe.case_id,
                    MIN(oe.created_at) AS first_resolved_at
                FROM outbox_events oe
                WHERE oe.event_type = 'CASE_STATUS_CHANGED'
                  AND JSON_UNQUOTE(
                        JSON_EXTRACT(oe.payload, '$.payload.status')
                      ) = 'RESOLVED'
                GROUP BY oe.case_id
            )
            SELECT
                u.id AS agentId,
                u.email AS email,

                COUNT(c.id) AS totalCasesCreated,

                SUM(
                    CASE
                        WHEN fr.first_resolved_at IS NOT NULL THEN 1
                        ELSE 0
                    END
                ) AS totalCasesResolved,

                AVG(
                    CASE
                        WHEN fr.first_resolved_at IS NOT NULL
                        THEN TIMESTAMPDIFF(
                            SECOND,
                            c.created_at,
                            fr.first_resolved_at
                        )
                        ELSE NULL
                    END
                ) AS averageResolutionSeconds

            FROM users u

            LEFT JOIN cases c
                ON c.created_by = u.id
                AND c.deleted = false
                AND (:createdAtFrom IS NULL
                     OR c.created_at >= :createdAtFrom)
                AND (:createdAtTo IS NULL
                     OR c.created_at <= :createdAtTo)

            LEFT JOIN first_resolution fr
                ON fr.case_id = c.id

            WHERE u.role = 'AGENT'

            GROUP BY u.id, u.email

            ORDER BY u.email
            """, nativeQuery = true)
    List<AgentPerformanceProjection> findAgentPerformance(
            @Param("createdAtFrom") Instant createdAtFrom,
            @Param("createdAtTo") Instant createdAtTo
    );
}
