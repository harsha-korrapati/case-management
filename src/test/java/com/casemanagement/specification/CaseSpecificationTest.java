package com.casemanagement.specification;

import com.casemanagement.entity.Case;
import com.casemanagement.entity.User;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.CaseStatus;
import com.casemanagement.enums.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaseSpecificationTest {

    private Root<Case> root;
    private CriteriaBuilder criteriaBuilder;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        criteriaBuilder = mock(CriteriaBuilder.class);
    }

    @Test
    void hasStatus_whenStatusIsProvided_shouldCreateEqualPredicate() {
        Path<Object> statusPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("status")).thenReturn(statusPath);
        when(criteriaBuilder.equal(statusPath, CaseStatus.OPEN))
                .thenReturn(predicate);

        Predicate result = CaseSpecification.hasStatus(CaseStatus.OPEN)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).equal(statusPath, CaseStatus.OPEN);
    }

    @Test
    void hasStatus_whenStatusIsNull_shouldCreateConjunction() {
        Predicate predicate = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        Predicate result = CaseSpecification.hasStatus(null)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).conjunction();
    }

    @Test
    void hasPriority_whenPriorityIsProvided_shouldCreateEqualPredicate() {
        Path<Object> priorityPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("priority")).thenReturn(priorityPath);
        when(criteriaBuilder.equal(priorityPath, CasePriority.HIGH))
                .thenReturn(predicate);

        Predicate result = CaseSpecification.hasPriority(CasePriority.HIGH)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).equal(priorityPath, CasePriority.HIGH);
    }

    @Test
    void hasPriority_whenPriorityIsNull_shouldCreateConjunction() {
        Predicate predicate = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        Predicate result = CaseSpecification.hasPriority(null)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).conjunction();
    }

    @Test
    void createdBy_whenUserIdIsProvided_shouldCreateEqualPredicate() {
        UUID userId = UUID.randomUUID();

        Path<Object> createdByPath = mock(Path.class);
        Path<Object> userIdPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("createdBy")).thenReturn(createdByPath);
        when(createdByPath.get("id")).thenReturn(userIdPath);
        when(criteriaBuilder.equal(userIdPath, userId))
                .thenReturn(predicate);

        Predicate result = CaseSpecification.createdBy(userId)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).equal(userIdPath, userId);
    }

    @Test
    void createdBy_whenUserIdIsNull_shouldCreateConjunction() {
        Predicate predicate = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        Predicate result = CaseSpecification.createdBy(null)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).conjunction();
    }

    @Test
    void createdAtFrom_whenDateIsProvided_shouldCreateGreaterThanOrEqualPredicate() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");

        Path<Instant> createdAtPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.<Instant>get("createdAt")).thenReturn(createdAtPath);

        when(criteriaBuilder.greaterThanOrEqualTo(createdAtPath, from))
                .thenReturn(predicate);

        Predicate result = CaseSpecification.createdAtFrom(from)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).greaterThanOrEqualTo(createdAtPath, from);
    }

    @Test
    void createdAtFrom_whenDateIsNull_shouldCreateConjunction() {
        Predicate predicate = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        Predicate result = CaseSpecification.createdAtFrom(null)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).conjunction();
    }

    @Test
    void createdAtTo_whenDateIsProvided_shouldCreateLessThanOrEqualPredicate() {
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        Path<Instant> createdAtPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.<Instant>get("createdAt")).thenReturn(createdAtPath);

        when(criteriaBuilder.lessThanOrEqualTo(createdAtPath, to))
                .thenReturn(predicate);

        Predicate result = CaseSpecification.createdAtTo(to)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).lessThanOrEqualTo(createdAtPath, to);
    }

    @Test
    void createdAtTo_whenDateIsNull_shouldCreateConjunction() {
        Predicate predicate = mock(Predicate.class);

        when(criteriaBuilder.conjunction()).thenReturn(predicate);

        Predicate result = CaseSpecification.createdAtTo(null)
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).conjunction();
    }

    @Test
    void notDeleted_shouldCreateIsFalsePredicate() {
        Path<Boolean> deletedPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.<Boolean>get("deleted")).thenReturn(deletedPath);
        when(criteriaBuilder.isFalse(deletedPath)).thenReturn(predicate);

        Predicate result = CaseSpecification.notDeleted()
                .toPredicate(root, null, criteriaBuilder);

        assertSame(predicate, result);
        verify(criteriaBuilder).isFalse(deletedPath);
    }
}