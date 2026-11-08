package com.casemanagement.repository;

import com.casemanagement.entity.CaseComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseCommentRepository extends JpaRepository<CaseComment, UUID> {

    Page<CaseComment> findByCaseId_IdAndCaseId_DeletedFalse(UUID caseId, Pageable pageable);
}
