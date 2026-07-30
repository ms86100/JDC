package com.avionics_systems.workflow.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.workflow.config.PatchCapableRestTemplate;
import com.avionics_systems.workflow.entity.WorkflowValidator;
import com.avionics_systems.workflow.repository.WorkflowValidatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Client for workflow validation - can be called from avionics-systems-issue-service to validate
 * before allowing transition completion.
 * This provides a synchronous validation API for external services.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowValidationClient {

    private final WorkflowValidatorRepository validatorRepository;
    private final WorkflowValidationService validationService;
    private final PatchCapableRestTemplate patchCapableRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate restTemplate() {
        return patchCapableRestTemplate.get();
    }

    @Value("${avionics-systems.services.workflow-url:http://localhost:8087}")
    private String workflowServiceUrl;

    /**
     * Validates a transition by transition ID using context map from the caller.
     * This is the main entry point for external services like avionics-systems-issue-service.
     *
     * @param transitionId The transition to validate
     * @param contextMap   Map containing:
     *                     - "userId" - current user UUID
     *                     - "fields" - Map of field name to value
     *                     - "comment" - Optional transition comment
     *                     - "attachments" - List of attachment data
     *                     - "linkedIssues" - Map of link type to issue data
     *                     - "subtasks" - List of subtask issue data
     *                     - "originalEstimate" - for time tracking
     *                     - "remainingEstimate" - for time tracking
     * @return ValidationResult indicating success or failure with errors
     */
    public ValidationResult validateTransition(UUID transitionId, Map<String, Object> contextMap) {
        if (transitionId == null) {
            log.warn("Cannot validate transition: transitionId is null");
            return ValidationResult.error(ValidationError.generalError(
                    null, "TRANSITION_ID_REQUIRED", "Transition ID is required for validation"));
        }

        // Load validators for this transition
        List<WorkflowValidator> validators = validatorRepository.findByTransitionIdOrderBySequenceAsc(transitionId);
        if (validators.isEmpty()) {
            log.debug("No validators configured for transition {}", transitionId);
            return ValidationResult.success();
        }

        // Convert context map to ValidatorExecutionContext
        ValidatorExecutionContext context = buildContext(transitionId, contextMap);

        // Execute validation
        return validationService.validateTransition(validators, context);
    }

    /**
     * Validates a transition synchronously via HTTP call.
     * This can be used when the caller is not in the same service.
     *
     * @param transitionId The transition to validate
     * @param contextMap   Context map as described above
     * @return true if validation passed, false otherwise
     */
    public boolean validateTransitionSync(UUID transitionId, Map<String, Object> contextMap) {
        ValidationResult result = validateTransition(transitionId, contextMap);
        return result.isValid();
    }

    /**
     * Gets all validation errors without throwing.
     *
     * @param transitionId The transition to validate
     * @param contextMap   Context map
     * @return List of validation errors (empty if valid)
     */
    public List<ValidationError> getValidationErrors(UUID transitionId, Map<String, Object> contextMap) {
        ValidationResult result = validateTransition(transitionId, contextMap);
        return result.allCollect();
    }

    /**
     * Builds a ValidatorExecutionContext from a context map.
     */
    @SuppressWarnings("unchecked")
    private ValidatorExecutionContext buildContext(UUID transitionId, Map<String, Object> contextMap) {
        UUID userId = parseUuid(contextMap.get("userId"));
        UUID issueId = parseUuid(contextMap.get("issueId"));
        UUID projectId = parseUuid(contextMap.get("projectId"));

        Map<String, Object> fields = contextMap.get("fields") instanceof Map
                ? (Map<String, Object>) contextMap.get("fields")
                : Map.of();

        Optional<String> comment = Optional.empty();
        Object commentObj = contextMap.get("comment");
        if (commentObj != null) {
            comment = Optional.of(commentObj.toString());
        }

        List<ValidatorExecutionContext.Attachment> attachments = parseAttachments(
                contextMap.get("attachments"));
        Map<String, ValidatorExecutionContext.Issue> linkedIssues = parseLinkedIssues(
                contextMap.get("linkedIssues"));
        List<ValidatorExecutionContext.Issue> subtasks = parseSubtasks(
                contextMap.get("subtasks"));

        Long originalEstimate = parseLong(contextMap.get("originalEstimate"));
        Long remainingEstimate = parseLong(contextMap.get("remainingEstimate"));

        return ValidatorExecutionContext.builder()
                .currentUserId(userId)
                .issueId(issueId)
                .projectId(projectId)
                .transitionId(transitionId)
                .issueFields(fields)
                .comment(comment)
                .attachments(attachments)
                .linkedIssues(linkedIssues)
                .subtasks(subtasks)
                .originalEstimate(originalEstimate)
                .remainingEstimate(remainingEstimate)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ValidatorExecutionContext.Attachment> parseAttachments(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> {
                        Map<String, Object> m = (Map<String, Object>) item;
                        return ValidatorExecutionContext.Attachment.builder()
                                .id(parseUuid(m.get("id")))
                                .filename(m.get("filename") != null ? m.get("filename").toString() : null)
                                .mimeType(m.get("mimeType") != null ? m.get("mimeType").toString() : null)
                                .size(parseLong(m.get("size")))
                                .authorId(parseUuid(m.get("authorId")))
                                .build();
                    })
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ValidatorExecutionContext.Issue> parseLinkedIssues(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        if (raw instanceof Map<?, ?> map) {
            Map<String, ValidatorExecutionContext.Issue> result = new HashMap<>();
            map.forEach((key, value) -> {
                if (value instanceof Map<?, ?> m) {
                    result.put(key.toString(), parseIssue(m));
                }
            });
            return result;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<ValidatorExecutionContext.Issue> parseSubtasks(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> parseIssue((Map<?, ?>) item))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private ValidatorExecutionContext.Issue parseIssue(Map<?, ?> m) {
        return ValidatorExecutionContext.Issue.builder()
                .id(parseUuid(m.get("id")))
                .key(m.get("key") != null ? m.get("key").toString() : null)
                .summary(m.get("summary") != null ? m.get("summary").toString() : null)
                .statusId(parseUuid(m.get("statusId")))
                .statusName(m.get("statusName") != null ? m.get("statusName").toString() : null)
                .resolutionId(parseUuid(m.get("resolutionId")))
                .resolutionName(m.get("resolutionName") != null ? m.get("resolutionName").toString() : null)
                .assigneeId(parseUuid(m.get("assigneeId")))
                .reporterId(parseUuid(m.get("reporterId")))
                .build();
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            log.warn("Could not parse UUID from: {}", value);
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}