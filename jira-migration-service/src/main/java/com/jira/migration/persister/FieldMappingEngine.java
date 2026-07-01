package com.jira.migration.persister;

import com.jira.migration.dto.ValidationResult;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.entity.UserMapping;
import com.jira.migration.exception.*;
import com.jira.migration.repository.ProjectMappingRepository;
import com.jira.migration.repository.UserMappingRepository;
import com.jira.migration.service.AuditService;
import com.jira.migration.service.UserDirectoryMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Field Mapping Engine
 * Handles transformation of source fields to target fields with:
 * - Default values
 * - Value transformations
 * - Lookup resolutions
 * - Custom field type conversions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FieldMappingEngine {

    private final ProjectMappingRepository projectMappingRepository;
    private final UserMappingRepository userMappingRepository;
    private final UserDirectoryMappingService userDirectoryMappingService;
    private final AuditService auditService;

    // Supported transformers
    private static final Map<String, FieldTransformer> TRANSFORMERS;

    static {
        Map<String, FieldTransformer> map = new HashMap<>();
        map.put("UPPERCASE", value -> value != null && value instanceof String ? ((String) value).toUpperCase() : value);
        map.put("LOWERCASE", value -> value != null && value instanceof String ? ((String) value).toLowerCase() : value);
        map.put("TRIM", value -> value != null && value instanceof String ? ((String) value).trim() : value);
        map.put("STRIP_HTML", value -> value != null && value instanceof String ? ((String) value).replaceAll("<[^>]*>", "").trim() : value);
        map.put("TO_INT", value -> {
            if (value == null || !(value instanceof String) || ((String) value).isBlank()) return null;
            try { return Integer.parseInt((String) value); } catch (NumberFormatException e) { return null; }
        });
        map.put("TO_BOOLEAN", value -> {
            if (value == null || !(value instanceof String)) return null;
            String s = (String) value;
            return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") ||
                   s.equalsIgnoreCase("1") || s.equalsIgnoreCase("y");
        });
        map.put("NULL_IF_EMPTY", value -> (value == null || (value instanceof String && ((String) value).isBlank())) ? null : value);
        map.put("DEFAULT_IF_NULL", value -> value);
        TRANSFORMERS = Collections.unmodifiableMap(map);
    }

    @FunctionalInterface
    public interface FieldTransformer {
        Object transform(Object value);
    }

    /**
     * Transform a single field value
     */
    public Object transformField(Object value, String transformerType) {
        if (transformerType == null || transformerType.isBlank()) {
            return value;
        }

        FieldTransformer transformer = TRANSFORMERS.get(transformerType.toUpperCase());
        if (transformer != null) {
            return transformer.transform(value);
        }

        // Handle parameterized transformers
        if (transformerType.startsWith("DEFAULT:")) {
            String defaultValue = transformerType.substring(8);
            return (value == null || value.toString().isBlank()) ? defaultValue : value;
        }

        return value;
    }

    /**
     * Apply complete field mapping to a row
     */
    public Map<String, Object> applyMapping(
            Map<String, String> sourceRow,
            List<FieldMappingConfig> mappings,
            UUID jobId,
            String entityType) {

        Map<String, Object> result = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        for (FieldMappingConfig mapping : mappings) {
            Object sourceValue = sourceRow.get(mapping.getSourceField().toLowerCase());
            Object transformedValue = sourceValue;

            // Apply transformer if specified
            if (mapping.getTransformer() != null) {
                transformedValue = transformField(sourceValue, mapping.getTransformer());
            }

            // Apply default if null
            if (transformedValue == null && mapping.getDefaultValue() != null) {
                transformedValue = mapping.getDefaultValue();
            }

            // Resolve references
            transformedValue = resolveReference(transformedValue, mapping, jobId);

            result.put(mapping.getTargetField(), transformedValue);

            // Track warnings
            if (sourceValue != null && transformedValue == null && !mapping.isRequired()) {
                warnings.add(String.format("Field '%s': value '%s' could not be resolved",
                        mapping.getSourceField(), sourceValue));
            }
        }

        if (!warnings.isEmpty()) {
            log.warn("Mapping warnings for {}: {}", entityType, warnings);
        }

        return result;
    }

    /**
     * Resolve foreign key references
     */
    private Object resolveReference(Object value, FieldMappingConfig mapping, UUID jobId) {
        if (value == null) return null;

        String resolvedValue = value.toString();

        switch (mapping.getReferenceType()) {
            case "USER":
                return resolveUserReference(resolvedValue, jobId);
            case "PROJECT":
                return resolveProjectReference(resolvedValue, jobId);
            case "ISSUE":
                return resolveIssueReference(resolvedValue, jobId);
            case "STATUS":
                return resolveStatusReference(resolvedValue);
            case "PRIORITY":
                return resolvePriorityReference(resolvedValue);
            case "ISSUE_TYPE":
                return resolveIssueTypeReference(resolvedValue);
            default:
                return resolvedValue;
        }
    }

    private UUID resolveUserReference(String usernameOrEmail, UUID jobId) {
        // Check if already mapped
        Optional<UserMapping> existing = userMappingRepository.findByJobIdAndSourceIdentifier(jobId, usernameOrEmail);
        if (existing.isPresent() && existing.get().getTargetUserId() != null) {
            return existing.get().getTargetUserId();
        }

        UUID resolved = userDirectoryMappingService.resolveToTargetUserId(usernameOrEmail, jobId);
        if (resolved != null) {
            return resolved;
        }
        log.debug("User reference unresolved: {}", usernameOrEmail);
        return null;
    }

    private String resolveProjectReference(String projectKey, UUID jobId) {
        Optional<ProjectMapping> existing = projectMappingRepository.findByJobIdAndSourceKey(jobId, projectKey);
        if (existing.isPresent()) {
            return existing.get().getTargetKey();
        }

        // TODO: Create new project mapping if needed
        return projectKey;
    }

    private String resolveIssueReference(String issueKey, UUID jobId) {
        // TODO: Query issue-service to check if issue exists
        return issueKey;
    }

    private String resolveStatusReference(String statusName) {
        // TODO: Query issue-service for status mapping
        return statusName;
    }

    private String resolvePriorityReference(String priorityName) {
        // TODO: Query issue-service for priority mapping
        return priorityName;
    }

    private String resolveIssueTypeReference(String issueTypeName) {
        // TODO: Query issue-service for issue type mapping
        return issueTypeName;
    }

    /**
     * Validate that all required fields are mapped
     */
    public ValidationResult validateRequiredMapping(
            Map<String, String> row,
            List<String> requiredFields) {

        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        List<ValidationResult.ValidationWarning> warnings = new ArrayList<>();

        for (String field : requiredFields) {
            String value = row.get(field.toLowerCase());
            if (value == null || value.isBlank()) {
                errors.add(ValidationResult.ValidationError.builder()
                        .field(field)
                        .errorCode("REQUIRED_FIELD_MISSING")
                        .message("Required field '" + field + "' is missing")
                        .build());
            }
        }

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FieldMappingConfig {
        private String sourceField;
        private String targetField;
        private String transformer;
        private Object defaultValue;
        private boolean required;
        private String referenceType; // USER, PROJECT, ISSUE, STATUS, PRIORITY, ISSUE_TYPE
        private Map<String, String> options;
    }
}