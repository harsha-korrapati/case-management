package com.casemanagement.service;

import com.casemanagement.dto.request.CaseFilterRequest;
import com.casemanagement.dto.request.CreateCaseRequest;
import com.casemanagement.dto.request.UpdateCaseRequest;
import com.casemanagement.dto.response.CaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CaseService {

    CaseResponse createCase(CreateCaseRequest request, UUID currentUserId, String idempotencyKey);

    CaseResponse getCaseById(UUID id);

    Page<CaseResponse> getCases(CaseFilterRequest filter, Pageable pageable);

    CaseResponse updateCase(UUID id, UpdateCaseRequest request, UUID currentUserId);

    void deleteCase(UUID id, UUID currentUserId);
}
