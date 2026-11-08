package com.casemanagement.service.impl;

import com.casemanagement.config.ApplicationMetrics;
import com.casemanagement.dto.request.CaseFilterRequest;
import com.casemanagement.dto.request.CreateCaseRequest;
import com.casemanagement.dto.request.UpdateCaseRequest;
import com.casemanagement.dto.response.CaseResponse;
import com.casemanagement.entity.Case;
import com.casemanagement.entity.IdempotencyRecord;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CaseEventType;
import com.casemanagement.enums.UserRole;
import com.casemanagement.exception.ForbiddenException;
import com.casemanagement.exception.IdempotencyConflictException;
import com.casemanagement.exception.ResourceNotFoundException;
import com.casemanagement.repository.CaseRepository;
import com.casemanagement.repository.IdempotencyRecordRepository;
import com.casemanagement.repository.UserRepository;
import com.casemanagement.service.CaseService;
import com.casemanagement.specification.CaseSpecification;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.time.ZoneOffset;
import java.util.UUID;
import com.casemanagement.entity.CaseEvent;
import com.casemanagement.event.CorrelationIdProvider;
import com.casemanagement.entity.OutboxEvent;
import com.casemanagement.repository.OutboxEventRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CaseServiceImpl implements CaseService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CorrelationIdProvider correlationIdProvider;
    private final ApplicationMetrics applicationMetrics;

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;
    private final JsonMapper jsonMapper;

    // CREATE CASE
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Override
    public CaseResponse createCase(CreateCaseRequest request, UUID currentUserId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        if (idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 255 characters");
        }

        User user = getUser(currentUserId);

        if (!hasRole(user, UserRole.ADMIN) && !hasRole(user, UserRole.AGENT)) {
            throw new ForbiddenException("You are not authorized to create cases");
        }

        String requestHash = generateRequestHash(request);

        // Check whether this user has already used this key.
        var existingRecord = idempotencyRecordRepository.findByUser_IdAndIdempotencyKey(
                        currentUserId,
                        idempotencyKey
                );

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();

            // Expired key: remove it so it can be reused.
            if (!record.getExpiresAt().isAfter(Instant.now())) {
                idempotencyRecordRepository.delete(record);
                idempotencyRecordRepository.flush();
            } else {
                // Same key, different request body.
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException("Idempotency key has already been used with a different request");
                }

                // Same key and same request: replay original response.
                return deserializeResponse(record.getResponseBody());
            }
        }

        // No active idempotency record: create a new case.
        Case caseEntity = new Case(request.getTitle(), request.getDescription(), request.getPriority(), user);

        Case savedCase = caseRepository.save(caseEntity);

        // Create CASE_CREATED event in the same transaction.
        saveCaseEvent(
                savedCase,
                CaseEventType.CASE_CREATED,
                Map.of(
                        "priority", savedCase.getPriority().name(),
                        "status", savedCase.getStatus().name()
                )
        );

        applicationMetrics.incrementCasesCreated();

        CaseResponse response = CaseResponse.from(savedCase);

        // Convert response DTO to JSON.
        String responseJson = serializeResponse(response);

        // Store idempotency record.
        IdempotencyRecord newRecord = new IdempotencyRecord(
                user,
                idempotencyKey,
                requestHash,
                responseJson,
                201,
                Instant.now().plusSeconds(86400)
        );

        idempotencyRecordRepository.save(newRecord);

        return response;
    }

    // GET CASE BY ID
    @PreAuthorize("isAuthenticated()")
    @Override
    @Transactional(readOnly = true)
    public CaseResponse getCaseById(UUID id) {

        Case caseEntity = getActiveCase(id);

        return CaseResponse.from(caseEntity);
    }

    // GET CASES WITH FILTERS, PAGINATION AND SORTING
    @PreAuthorize("isAuthenticated()")
    @Override
    @Transactional(readOnly = true)
    public Page<CaseResponse> getCases(CaseFilterRequest filter, Pageable pageable) {

        Specification<Case> specification =
                Specification.where(CaseSpecification.notDeleted())
                        .and(CaseSpecification.hasStatus(filter.getStatus()))
                        .and(CaseSpecification.hasPriority(filter.getPriority()))
                        .and(CaseSpecification.createdBy(filter.getCreatedBy()))
                        .and(CaseSpecification.createdAtFrom(
                                filter.getCreatedAtFrom() == null
                                        ? null
                                        : filter.getCreatedAtFrom()
                                        .toInstant(ZoneOffset.UTC)
                        ))
                        .and(CaseSpecification.createdAtTo(
                                filter.getCreatedAtTo() == null
                                        ? null
                                        : filter.getCreatedAtTo()
                                        .toInstant(ZoneOffset.UTC)
                        ));

        Page<Case> cases = caseRepository.findAll(specification, pageable);

        return cases.map(CaseResponse::from);
    }

    // UPDATE CASE
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Override
    public CaseResponse updateCase(UUID id, UpdateCaseRequest request, UUID currentUserId) {

        Case caseEntity = getActiveCase(id);
        User user = getUser(currentUserId);
        var previousStatus = caseEntity.getStatus();

        boolean isAdmin = hasRole(user, UserRole.ADMIN);
        boolean isAgent = hasRole(user, UserRole.AGENT);

        boolean isCreator = caseEntity.getCreatedBy()
                .getId()
                .equals(currentUserId);

        // Reject an empty PATCH request
        if (!request.isTitleProvided()
                && !request.isDescriptionProvided()
                && !request.isStatusProvided()
                && !request.isPriorityProvided()) {

            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        // TITLE
        if (request.isTitleProvided()) {

            if (!isAdmin && !isCreator) {
                throw new ForbiddenException("Only the case creator or ADMIN can update the title");
            }

            if (request.getTitle() == null || request.getTitle().isBlank()) {
                throw new IllegalArgumentException("Title cannot be null or blank");
            }

            if (request.getTitle().length() > 150) {
                throw new IllegalArgumentException("Title cannot exceed 150 characters");
            }

            caseEntity.setTitle(request.getTitle());
        }

        // DESCRIPTION
        if (request.isDescriptionProvided()) {

            if (!isAdmin && !isCreator) {
                throw new ForbiddenException("Only the case creator or ADMIN can update the description");
            }

            if (request.getDescription() == null
                    || request.getDescription().isBlank()) {
                throw new IllegalArgumentException("Description cannot be null or blank");
            }

            if (request.getDescription().length() > 2000) {
                throw new IllegalArgumentException("Description cannot exceed 2000 characters");
            }

            caseEntity.setDescription(request.getDescription());
        }

        // STATUS
        if (request.isStatusProvided()) {

            if (!isAdmin && !isAgent) {
                throw new ForbiddenException("Only ADMIN or AGENT can update case status");
            }

            if (request.getStatus() == null) {
                throw new IllegalArgumentException("Status cannot be null");
            }

            caseEntity.changeStatus(request.getStatus());
        }

        // PRIORITY
        if (request.isPriorityProvided()) {

            if (!isAdmin) {
                throw new ForbiddenException("Only ADMIN can update case priority");
            }

            if (request.getPriority() == null) {
                throw new IllegalArgumentException("Priority cannot be null");
            }

            caseEntity.changePriority(request.getPriority());
        }

        Case updatedCase = caseRepository.save(caseEntity);

        boolean statusChanged = previousStatus != updatedCase.getStatus();

        CaseEventType eventType = statusChanged ? CaseEventType.CASE_STATUS_CHANGED : CaseEventType.CASE_UPDATED;

        Map<String, Object> payload;

        if (statusChanged) {
            payload = Map.of(
                    "previousStatus", previousStatus.name(),
                    "status", updatedCase.getStatus().name(),
                    "priority", updatedCase.getPriority().name()
            );
        } else {
            payload = Map.of(
                    "status", updatedCase.getStatus().name(),
                    "priority", updatedCase.getPriority().name()
            );
        }

        saveCaseEvent(updatedCase, eventType, payload);
        applicationMetrics.incrementCasesUpdated();

        return CaseResponse.from(updatedCase);
    }

    // SOFT DELETE CASE
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void deleteCase(UUID id, UUID currentUserId) {

        User user = getUser(currentUserId);

        if (!hasRole(user, UserRole.ADMIN)) {
            throw new ForbiddenException("Only ADMIN can delete cases");
        }

        Case caseEntity = getActiveCase(id);

        caseEntity.markDeleted(Instant.now());

        caseRepository.save(caseEntity);
    }

    // HELPER METHODS

    private User getUser(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Case getActiveCase(UUID id) {

        return caseRepository.findById(id)
                .filter(caseEntity -> !caseEntity.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));
    }

    private boolean hasRole(User user, UserRole... allowedRoles) {

        for (UserRole role : allowedRoles) {

            if (user.getRole() == role) {
                return true;
            }
        }

        return false;
    }

    private String generateRequestHash(CreateCaseRequest request) {

        try {
            byte[] requestBytes = jsonMapper.writeValueAsBytes(request);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(requestBytes);

            return HexFormat.of().formatHex(hashBytes);

        } catch (JacksonException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to generate request hash", ex);
        }
    }

    private String serializeResponse(CaseResponse response) {

        try {
            return jsonMapper.writeValueAsString(response);

        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to serialize case response", ex);
        }
    }

    private CaseResponse deserializeResponse(String responseJson) {

        try {
            return jsonMapper.readValue(
                    responseJson,
                    CaseResponse.class
            );

        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to deserialize stored case response", ex);
        }
    }

    private void saveCaseEvent(Case caseEntity, CaseEventType eventType, Map<String, Object> payload) {
        CaseEvent event = new CaseEvent(
                UUID.randomUUID(),
                eventType,
                caseEntity.getId(),
                Instant.now(),
                payload,
                correlationIdProvider.getCorrelationId()
        );

        try {
            String eventJson = jsonMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(event, eventJson);

            outboxEventRepository.save(outboxEvent);

        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize case event", exception);
        }
    }
}