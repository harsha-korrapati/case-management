package com.casemanagement.service.impl;
import com.casemanagement.config.ApplicationMetrics;
import com.casemanagement.dto.request.CaseFilterRequest;
import com.casemanagement.dto.request.UpdateCaseRequest;
import com.casemanagement.entity.IdempotencyRecord;
import com.casemanagement.event.CorrelationIdProvider;
import com.casemanagement.exception.IdempotencyConflictException;
import com.casemanagement.exception.ResourceNotFoundException;
import com.casemanagement.repository.CaseRepository;
import com.casemanagement.repository.IdempotencyRecordRepository;
import com.casemanagement.repository.OutboxEventRepository;
import com.casemanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.casemanagement.dto.request.CreateCaseRequest;
import com.casemanagement.dto.response.CaseResponse;
import com.casemanagement.entity.Case;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.CaseStatus;
import com.casemanagement.enums.UserRole;
import com.casemanagement.exception.ForbiddenException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CaseServiceImplTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CorrelationIdProvider correlationIdProvider;

    @Mock
    private ApplicationMetrics applicationMetrics;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private CaseServiceImpl caseService;

    @Test
    void createCase_shouldThrowException_whenIdempotencyKeyIsNull() {

        UUID userId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> caseService.createCase(null, userId, null)
        );

        verifyNoInteractions(
                caseRepository,
                userRepository,
                outboxEventRepository,
                correlationIdProvider,
                applicationMetrics,
                idempotencyRecordRepository,
                objectMapper,
                jsonMapper
        );
    }

    @Test
    void createCase_shouldThrowException_whenIdempotencyKeyIsBlank() {

        UUID userId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> caseService.createCase(null, userId, "   ")
        );

        verifyNoInteractions(
                caseRepository,
                userRepository,
                outboxEventRepository,
                correlationIdProvider,
                applicationMetrics,
                idempotencyRecordRepository,
                objectMapper,
                jsonMapper
        );
    }

    @Test
    void createCase_shouldThrowException_whenIdempotencyKeyExceeds255Characters() {

        UUID userId = UUID.randomUUID();
        String idempotencyKey = "A".repeat(256);

        assertThrows(
                IllegalArgumentException.class,
                () -> caseService.createCase(null, userId, idempotencyKey)
        );

        verifyNoInteractions(
                caseRepository,
                userRepository,
                outboxEventRepository,
                correlationIdProvider,
                applicationMetrics,
                idempotencyRecordRepository,
                objectMapper,
                jsonMapper
        );
    }

    @Test
    void createCase_shouldCreateCaseSuccessfully_whenUserIsAgent() throws Exception {

        // Arrange
        UUID userId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        User user = new User("agent@test.com", "password", UserRole.AGENT);

        ReflectionTestUtils.setField(user, "id", userId);

        CreateCaseRequest request = new CreateCaseRequest();
        request.setTitle("Payment issue");
        request.setDescription("Customer reported a payment issue");
        request.setPriority(CasePriority.HIGH);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(jsonMapper.writeValueAsBytes(request)).thenReturn("request-hash-input".getBytes());

        when(idempotencyRecordRepository
                .findByUser_IdAndIdempotencyKey(userId, "key-123"))
                .thenReturn(Optional.empty());

        when(correlationIdProvider.getCorrelationId()).thenReturn("correlation-123");

        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");

        when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> {

                    Case savedCase = invocation.getArgument(0);

                    ReflectionTestUtils.setField(savedCase, "id", caseId);
                    ReflectionTestUtils.setField(
                            savedCase, "createdAt", Instant.now());
                    ReflectionTestUtils.setField(
                            savedCase, "updatedAt", Instant.now());

                    return savedCase;
                });

        // Act
        CaseResponse response = caseService.createCase(request, userId, "key-123");

        // Assert
        assertNotNull(response);
        assertEquals(caseId, response.getId());
        assertEquals("Payment issue", response.getTitle());
        assertEquals("Customer reported a payment issue",
                response.getDescription());
        assertEquals(CasePriority.HIGH, response.getPriority());
        assertEquals(CaseStatus.OPEN, response.getStatus());
        assertEquals(userId, response.getCreatedBy());

        verify(caseRepository).save(any(Case.class));
        verify(outboxEventRepository).save(any());
        verify(idempotencyRecordRepository).save(any());
        verify(applicationMetrics).incrementCasesCreated();
    }

    @Test
    void createCase_shouldThrowForbiddenException_whenUserIsViewer() {

        // Arrange
        UUID userId = UUID.randomUUID();

        User viewer = new User("viewer@test.com", "password", UserRole.VIEWER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(viewer));

        CreateCaseRequest request = new CreateCaseRequest();
        request.setTitle("Test case");
        request.setDescription("Test description");
        request.setPriority(CasePriority.HIGH);

        // Act & Assert
        assertThrows(
                ForbiddenException.class,
                () -> caseService.createCase(
                        request,
                        userId,
                        "key-123"
                )
        );

        verify(userRepository).findById(userId);

        verifyNoInteractions(
                caseRepository,
                outboxEventRepository,
                idempotencyRecordRepository,
                applicationMetrics,
                correlationIdProvider,
                jsonMapper
        );
    }

    @Test
    void createCase_shouldReturnExistingResponse_whenSameIdempotencyKeyAndRequest() throws Exception {

        // Arrange
        UUID userId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        User user = new User("agent@test.com", "password", UserRole.AGENT);

        CreateCaseRequest request = new CreateCaseRequest();
        request.setTitle("Payment issue");
        request.setDescription("Customer reported a payment issue");
        request.setPriority(CasePriority.HIGH);

        String requestHash = "same-request-hash";
        String storedResponse = """
            {
              "id": "%s",
              "title": "Payment issue",
              "description": "Customer reported a payment issue",
              "status": "OPEN",
              "priority": "HIGH",
              "createdBy": "%s"
            }
            """.formatted(caseId, userId);

        IdempotencyRecord existingRecord = new IdempotencyRecord(
                user,
                "key-123",
                requestHash,
                storedResponse,
                201,
                Instant.now().plusSeconds(3600)
        );

        CaseResponse expectedResponse = CaseResponse.builder()
                .id(caseId)
                .title("Payment issue")
                .description("Customer reported a payment issue")
                .status(CaseStatus.OPEN)
                .priority(CasePriority.HIGH)
                .createdBy(userId)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(jsonMapper.writeValueAsBytes(request)).thenReturn("same-request-hash-input".getBytes());

        // The service calculates SHA-256 from the request bytes.
        // We'll stub the existing record with the calculated hash below.
        String calculatedHash = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                        .digest("same-request-hash-input".getBytes())
        );

        IdempotencyRecord matchingRecord = new IdempotencyRecord(
                user,
                "key-123",
                calculatedHash,
                storedResponse,
                201,
                Instant.now().plusSeconds(3600)
        );

        when(idempotencyRecordRepository
                .findByUser_IdAndIdempotencyKey(userId, "key-123"))
                .thenReturn(Optional.of(matchingRecord));

        when(jsonMapper.readValue(storedResponse, CaseResponse.class)).thenReturn(expectedResponse);

        // Act
        CaseResponse actualResponse = caseService.createCase(request, userId, "key-123");

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getId(), actualResponse.getId());
        assertEquals(expectedResponse.getTitle(), actualResponse.getTitle());
        assertEquals(expectedResponse.getStatus(), actualResponse.getStatus());

        // Replay must not create another case or outbox event.
        verify(caseRepository, never()).save(any(Case.class));
        verify(outboxEventRepository, never()).save(any());
        verify(idempotencyRecordRepository, never()).save(any());
        verifyNoInteractions(applicationMetrics);
    }

    @Test
    void createCase_shouldThrowConflict_whenSameIdempotencyKeyUsedWithDifferentRequest() throws Exception {

        // Arrange
        UUID userId = UUID.randomUUID();

        User user = new User("agent@test.com", "password", UserRole.AGENT);

        CreateCaseRequest request = new CreateCaseRequest();
        request.setTitle("New payment issue");
        request.setDescription("Different payment issue");
        request.setPriority(CasePriority.HIGH);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(jsonMapper.writeValueAsBytes(request)).thenReturn("different-request-body".getBytes());

        IdempotencyRecord existingRecord = new IdempotencyRecord(
                user,
                "key-123",
                "original-request-hash",
                "{}",
                201,
                Instant.now().plusSeconds(3600)
        );

        when(idempotencyRecordRepository
                .findByUser_IdAndIdempotencyKey(userId, "key-123"))
                .thenReturn(Optional.of(existingRecord));

        // Act & Assert
        assertThrows(
                IdempotencyConflictException.class,
                () -> caseService.createCase(request, userId, "key-123")
        );

        verify(caseRepository, never()).save(any(Case.class));
        verify(outboxEventRepository, never()).save(any());
        verify(idempotencyRecordRepository, never()).save(any());
        verifyNoInteractions(applicationMetrics);
    }

    @Test
    void updateCase_shouldUpdateStatus_whenUserIsAgent() throws Exception {

        UUID agentId = UUID.randomUUID();

        User owner = new User("owner@test.com", "password", UserRole.AGENT);

        User agent = new User("agent@test.com", "password", UserRole.AGENT);

        Case existingCase = createTestCase(owner);

        UpdateCaseRequest request = new UpdateCaseRequest();
        request.setStatus(CaseStatus.IN_PROGRESS);

        when(caseRepository.findById(any(UUID.class))).thenReturn(Optional.of(existingCase));

        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));

        when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(correlationIdProvider.getCorrelationId()).thenReturn("correlation-123");

        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");

        CaseResponse response = caseService.updateCase(existingCase.getId(), request, agentId);

        assertNotNull(response);
        assertEquals(CaseStatus.IN_PROGRESS, response.getStatus());

        verify(caseRepository).save(existingCase);
        verify(outboxEventRepository).save(any());
        verify(applicationMetrics).incrementCasesUpdated();
    }

    @Test
    void updateCase_shouldThrowForbiddenException_whenAgentUpdatesPriority() {

        UUID agentId = UUID.randomUUID();

        User owner = new User("owner@test.com", "password", UserRole.AGENT);

        User agent = new User("agent@test.com", "password", UserRole.AGENT);

        Case existingCase = createTestCase(owner);

        UpdateCaseRequest request = new UpdateCaseRequest();
        request.setPriority(CasePriority.CRITICAL);

        when(caseRepository.findById(existingCase.getId())).thenReturn(Optional.of(existingCase));

        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));

        assertThrows(
                ForbiddenException.class,
                () -> caseService.updateCase(
                        existingCase.getId(), request, agentId
                )
        );

        verify(caseRepository, never()).save(any(Case.class));
        verifyNoInteractions(outboxEventRepository, applicationMetrics);
    }

    @Test
    void updateCase_shouldThrowForbiddenException_whenAgentUpdatesOtherUsersTitle() {

        UUID agentId = UUID.randomUUID();

        User owner = new User("owner@test.com", "password", UserRole.AGENT);

        User agent = new User("agent@test.com", "password", UserRole.AGENT);

        Case existingCase = createTestCase(owner);

        UpdateCaseRequest request = new UpdateCaseRequest();
        request.setTitle("Updated title");

        when(caseRepository.findById(existingCase.getId())).thenReturn(Optional.of(existingCase));

        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));

        assertThrows(
                ForbiddenException.class,
                () -> caseService.updateCase(
                        existingCase.getId(), request, agentId
                )
        );

        verify(caseRepository, never()).save(any(Case.class));
        verifyNoInteractions(outboxEventRepository, applicationMetrics);
    }

    @Test
    void updateCase_shouldThrowForbiddenException_whenUserIsViewer() {

        UUID viewerId = UUID.randomUUID();

        User viewer = new User("viewer@test.com", "password", UserRole.VIEWER);

        Case existingCase = createTestCase(viewer);

        UpdateCaseRequest request = new UpdateCaseRequest();
        request.setStatus(CaseStatus.RESOLVED);

        when(caseRepository.findById(existingCase.getId())).thenReturn(Optional.of(existingCase));

        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        assertThrows(
                ForbiddenException.class,
                () -> caseService.updateCase(
                        existingCase.getId(), request, viewerId
                )
        );

        verify(caseRepository, never()).save(any(Case.class));
        verifyNoInteractions(outboxEventRepository, applicationMetrics);
    }

    @Test
    void deleteCase_whenAdmin_shouldSoftDeleteCase() {
        UUID adminId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        User admin = new User("admin@test.com", "password", UserRole.ADMIN);
        ReflectionTestUtils.setField(admin, "id", adminId);

        Case existingCase = createTestCase(admin);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(caseRepository.save(any(Case.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        caseService.deleteCase(caseId, adminId);

        assertTrue(existingCase.isDeleted());
        assertNotNull(existingCase.getDeletedAt());

        verify(caseRepository).save(existingCase);
    }

    @Test
    void deleteCase_whenAgent_shouldThrowForbiddenException() {
        UUID agentId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        User agent = new User("agent@test.com", "password", UserRole.AGENT);
        ReflectionTestUtils.setField(agent, "id", agentId);

        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));

        assertThrows(
                ForbiddenException.class,
                () -> caseService.deleteCase(caseId, agentId)
        );

        verify(caseRepository, never()).save(any(Case.class));
    }

    @Test
    void deleteCase_whenCaseDoesNotExist_shouldThrowResourceNotFoundException() {
        UUID adminId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        User admin = new User("admin@test.com", "password", UserRole.ADMIN);
        ReflectionTestUtils.setField(admin, "id", adminId);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> caseService.deleteCase(caseId, adminId)
        );

        verify(caseRepository, never()).save(any(Case.class));
    }

    @Test
    void getCaseById_whenCaseExists_shouldReturnCaseResponse() {
        UUID userId = UUID.randomUUID();

        User owner = new User("agent@test.com", "password", UserRole.AGENT);
        ReflectionTestUtils.setField(owner, "id", userId);

        Case existingCase = createTestCase(owner);
        UUID caseId = existingCase.getId();

        when(caseRepository.findById(caseId))
                .thenReturn(Optional.of(existingCase));

        CaseResponse response = caseService.getCaseById(caseId);

        assertNotNull(response);
        assertEquals(caseId, response.getId());
        assertEquals("Existing case", response.getTitle());
        assertEquals(userId, response.getCreatedBy());

        verify(caseRepository).findById(caseId);
    }

    @Test
    void getCaseById_whenCaseDoesNotExist_shouldThrowResourceNotFoundException() {
        UUID caseId = UUID.randomUUID();

        when(caseRepository.findById(caseId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> caseService.getCaseById(caseId)
        );

        verify(caseRepository).findById(caseId);
    }

    @Test
    void getCaseById_whenCaseIsDeleted_shouldThrowResourceNotFoundException() {
        UUID userId = UUID.randomUUID();

        User owner = new User("agent@test.com", "password", UserRole.AGENT);
        ReflectionTestUtils.setField(owner, "id", userId);

        Case deletedCase = createTestCase(owner);
        UUID caseId = deletedCase.getId();

        deletedCase.markDeleted(Instant.now());

        when(caseRepository.findById(caseId))
                .thenReturn(Optional.of(deletedCase));

        assertThrows(
                ResourceNotFoundException.class,
                () -> caseService.getCaseById(caseId)
        );

        verify(caseRepository).findById(caseId);
    }

    @Test
    void getCases_whenCasesExist_shouldReturnPaginatedCaseResponses() {
        User owner = new User("agent@test.com", "password", UserRole.AGENT);
        ReflectionTestUtils.setField(owner, "id", UUID.randomUUID());

        Case firstCase = createTestCase(owner);
        Case secondCase = createTestCase(owner);

        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        Page<Case> casePage = new PageImpl<>(
                List.of(firstCase, secondCase),
                pageable,
                2
        );

        when(caseRepository.findAll(
                ArgumentMatchers.<Specification<Case>>any(),
                eq(pageable)
        )).thenReturn(casePage);

        Page<CaseResponse> response =
                caseService.getCases(new CaseFilterRequest(), pageable);

        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        assertEquals(2, response.getContent().size());
        assertEquals(firstCase.getId(), response.getContent().get(0).getId());
        assertEquals(secondCase.getId(), response.getContent().get(1).getId());

        verify(caseRepository).findAll(
                ArgumentMatchers.<Specification<Case>>any(),
                eq(pageable)
        );
    }

    @Test
    void getCases_whenNoCasesExist_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Case> emptyPage = Page.empty(pageable);

        when(caseRepository.findAll(
                ArgumentMatchers.<Specification<Case>>any(),
                eq(pageable)
        )).thenReturn(emptyPage);

        Page<CaseResponse> response =
                caseService.getCases(new CaseFilterRequest(), pageable);

        assertNotNull(response);
        assertTrue(response.isEmpty());
        assertEquals(0, response.getTotalElements());

        verify(caseRepository).findAll(
                ArgumentMatchers.<Specification<Case>>any(),
                eq(pageable)
        );
    }

    @Test
    void getCases_withFilters_shouldPassSpecificationAndPageableToRepository() {
        CaseFilterRequest filter = new CaseFilterRequest();
        filter.setStatus(CaseStatus.OPEN);
        filter.setPriority(CasePriority.HIGH);
        filter.setCreatedBy(UUID.randomUUID());
        filter.setCreatedAtFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        filter.setCreatedAtTo(LocalDateTime.of(2026, 1, 31, 23, 59));

        Pageable pageable = PageRequest.of(1, 5);

        when(caseRepository.findAll(
                ArgumentMatchers.<Specification<Case>>any(),
                eq(pageable)
        )).thenReturn(Page.empty(pageable));

        Page<CaseResponse> response = caseService.getCases(filter, pageable);

        assertNotNull(response);
        assertTrue(response.isEmpty());

        ArgumentCaptor<Specification<Case>> specificationCaptor =
                ArgumentCaptor.forClass(Specification.class);

        verify(caseRepository).findAll(
                specificationCaptor.capture(),
                eq(pageable)
        );

        assertNotNull(specificationCaptor.getValue());
    }


    private Case createTestCase(User owner) {

        // Simulate Hibernate-generated user ID
        if (owner.getId() == null) {
            ReflectionTestUtils.setField(owner, "id", UUID.randomUUID());
        }

        Case testCase = new Case("Existing case", "Existing description", CasePriority.MEDIUM, owner);

        ReflectionTestUtils.setField(testCase, "id", UUID.randomUUID());

        return testCase;
    }
}