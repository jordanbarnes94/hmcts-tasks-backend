package uk.gov.hmcts.reform.dev.modules.tasks.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.dev.modules.tasks.models.Task;
import uk.gov.hmcts.reform.dev.modules.tasks.models.TaskStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for building dynamic Task queries.
 * Each method returns a Specification that can be combined using .and() / .or()
 * Null parameters are handled gracefully - they simply don't add predicates to the query.
 */
public class TaskSpecifications {

    /**
     * Filter by task status.
     * @param status The status to filter by, or null to ignore this filter
     * @return Specification that filters by status, or null if status is null
     */
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;  // No filter - returns all tasks
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Search for text in title or description (case-insensitive).
     * @param searchText The text to search for, or null to ignore this filter
     * @return Specification that searches title and description, or null if searchText is null
     */
    public static Specification<Task> searchByText(String searchText) {
        return (root, query, criteriaBuilder) -> {
            if (searchText == null || searchText.isBlank()) {
                return null;  // No filter
            }

            // Escape LIKE wildcards so they are treated as literal characters
            String escaped = searchText.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

            String searchPattern = "%" + escaped + "%";
            Predicate titleMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")),
                searchPattern,
                '\\'
            );
            Predicate descriptionMatch = criteriaBuilder.like(
                criteriaBuilder.lower(root.get("description")),
                searchPattern,
                '\\'
            );

            return criteriaBuilder.or(titleMatch, descriptionMatch);
        };
    }

    /**
     * Filter by due date range (inclusive).
     * @param dueDateFrom Start of date range (inclusive), or null for no lower bound
     * @param dueDateTo End of date range (inclusive), or null for no upper bound
     * @return Specification that filters by date range, or null if both dates are null
     */
    public static Specification<Task> hasDueDateBetween(LocalDateTime dueDateFrom, LocalDateTime dueDateTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dueDateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dueDateFrom));
            }
            if (dueDateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dueDateTo));
            }

            if (predicates.isEmpty()) {
                return null;  // No filter
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Combine all filters into a single Specification.
     * @param status Optional status filter
     * @param searchText Optional text search
     * @param dueDateFrom Optional start of date range
     * @param dueDateTo Optional end of date range
     * @return Combined Specification with all filters applied
     */
    public static Specification<Task> withFilters(
        TaskStatus status,
        String searchText,
        LocalDateTime dueDateFrom,
        LocalDateTime dueDateTo
    ) {
        return Specification.allOf(
            hasStatus(status),
            searchByText(searchText),
            hasDueDateBetween(dueDateFrom, dueDateTo)
        );
    }
}
