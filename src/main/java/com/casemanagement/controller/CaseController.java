package com.casemanagement.controller;

import com.casemanagement.dto.request.CaseFilterRequest;
import com.casemanagement.dto.request.CreateCaseRequest;
import com.casemanagement.dto.request.UpdateCaseRequest;
import com.casemanagement.dto.response.CaseResponse;
import com.casemanagement.security.AuthenticatedUser;
import com.casemanagement.service.CaseService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CaseController {

    private final CaseService caseService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "title", "priority", "status");

    // CREATE CASE
    @PostMapping
    public ResponseEntity<CaseResponse> createCase(
            @Valid @RequestBody CreateCaseRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {

        CaseResponse response = caseService.createCase(request, currentUser.getId(), idempotencyKey);

        URI location = URI.create("/cases/" + response.getId());

        return ResponseEntity.created(location).body(response);
    }

    // GET CASE BY ID
    @GetMapping("/{id}")
    public ResponseEntity<CaseResponse> getCaseById(@PathVariable UUID id) {

        return ResponseEntity.ok(caseService.getCaseById(id));
    }

    // GET CASES WITH FILTERS, PAGINATION AND SORTING
    @GetMapping
    public ResponseEntity<Page<CaseResponse>> getCases(
            @Valid @ModelAttribute CaseFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {

        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Direction must be ASC or DESC");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        return ResponseEntity.ok(caseService.getCases(filter, pageable));
    }

    // UPDATE CASE
    @PatchMapping("/{id}")
    public ResponseEntity<CaseResponse> updateCase(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {

        CaseResponse response = caseService.updateCase(id, request, currentUser.getId());

        return ResponseEntity.ok(response);
    }

    // SOFT DELETE CASE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCase(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser currentUser) {

        caseService.deleteCase(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
