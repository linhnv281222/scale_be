package org.facenet.common.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic specification builder for dynamic filtering
 */
public class GenericSpecification<T> {

    /**
     * Build specification from filter parameters
     */
    public Specification<T> buildSpecification(Map<String, String> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                if (value != null && !value.isBlank()) {
                    try {
                        // Handle nested properties (e.g., location.id, manufacturer.id)
                        String[] parts = key.split("\\.");
                        
                        // Handle different filter operations
                        if (key.endsWith("_like")) {
                            // LIKE operation for strings
                            String field = key.substring(0, key.length() - 5);
                            predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(getPath(root, field).as(String.class)),
                                "%" + value.toLowerCase() + "%"
                            ));
                        } else if (key.endsWith("_eq")) {
                            // Equals operation
                            String field = key.substring(0, key.length() - 3);
                            predicates.add(criteriaBuilder.equal(getPath(root, field), convertValue(value)));
                        } else if (key.endsWith("_gt")) {
                            // Greater than for numbers/dates
                            String field = key.substring(0, key.length() - 3);
                            predicates.add(criteriaBuilder.greaterThan(
                                getPath(root, field).as(String.class), value
                            ));
                        } else if (key.endsWith("_lt")) {
                            // Less than for numbers/dates
                            String field = key.substring(0, key.length() - 3);
                            predicates.add(criteriaBuilder.lessThan(
                                getPath(root, field).as(String.class), value
                            ));
                        } else if (key.endsWith("_gte")) {
                            // Greater than or equal
                            String field = key.substring(0, key.length() - 4);
                            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                getPath(root, field).as(String.class), value
                            ));
                        } else if (key.endsWith("_lte")) {
                            // Less than or equal
                            String field = key.substring(0, key.length() - 4);
                            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                getPath(root, field).as(String.class), value
                            ));
                        } else if (key.endsWith("_in")) {
                            // IN operation for lists (e.g., location.id_in=1,2,3)
                            String field = key.substring(0, key.length() - 3);
                            String[] values = value.split(",");
                            List<Object> convertedValues = new ArrayList<>();
                            for (String val : values) {
                                convertedValues.add(convertValue(val.trim()));
                            }
                            predicates.add(getPath(root, field).in(convertedValues));
                        } else {
                            // Default: equals operation
                            predicates.add(criteriaBuilder.equal(getPath(root, key), convertValue(value)));
                        }
                    } catch (Exception e) {
                        // Ignore invalid filter fields
                    }
                }
            });

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Get path for nested properties (e.g., location.id -> root.get("location").get("id"))
     */
    private jakarta.persistence.criteria.Path<?> getPath(
            jakarta.persistence.criteria.Root<T> root, String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        jakarta.persistence.criteria.Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }

    /**
     * Convert string value to appropriate type
     */
    private Object convertValue(String value) {
        if (value == null) return null;
        
        // Try boolean
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        
        // Try number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // Not a number, return as string
        }
        
        return value;
    }

    /**
     * Build search specification for multiple fields
     */
    public Specification<T> buildSearchSpecification(String search, String... fields) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            String searchPattern = "%" + search.toLowerCase() + "%";

            for (String field : fields) {
                try {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get(field).as(String.class)),
                        searchPattern
                    ));
                } catch (Exception e) {
                    // Ignore invalid fields
                }
            }

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
