package com.casemanagement.specification;

import com.casemanagement.entity.Case;
import com.casemanagement.enums.CasePriority;
import com.casemanagement.enums.CaseStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public class CaseSpecification {

    private CaseSpecification() {
    }

    public static Specification<Case> hasStatus(CaseStatus status) {

        return (root, query, criteriaBuilder) -> {

            if (status == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Case> hasPriority(CasePriority priority) {

        return (root, query, criteriaBuilder) -> {

            if (priority == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("priority"), priority);
        };
    }

    public static Specification<Case> createdBy(UUID userId) {

        return (root, query, criteriaBuilder) -> {

            if (userId == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("createdBy").get("id"), userId);
        };
    }

    public static Specification<Case> createdAtFrom(Instant from) {

        return (root, query, criteriaBuilder) -> {

            if (from == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from);
        };
    }

    public static Specification<Case> createdAtTo(Instant to) {

        return (root, query, criteriaBuilder) -> {

            if (to == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<Case> notDeleted() {

        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
    }
}
