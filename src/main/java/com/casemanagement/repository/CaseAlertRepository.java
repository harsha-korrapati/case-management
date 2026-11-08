package com.casemanagement.repository;

import com.casemanagement.entity.CaseAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CaseAlertRepository
        extends JpaRepository<CaseAlert, UUID> {

    boolean existsByEventId(UUID eventId);
}
