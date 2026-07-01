package com.jira.workflow.controller;

import com.jira.workflow.dto.ValidateTransitionResponse;
import com.jira.workflow.entity.WorkflowValidator;
import com.jira.workflow.repository.WorkflowValidatorRepository;
import com.jira.workflow.validation.ValidationError;
import com.jira.workflow.validation.ValidationResult;
import com.jira.workflow.validation.ValidatorExecutionContext;
import com.jira.workflow.validation.WorkflowValidationClient;
import com.jira.workflow.validation.WorkflowValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for workflow validation API.
 * Provides endpoints for validating transitions and validators.
 */
@RestController
@RequestMapping("/api/workflow/validation")
@RequiredArgsConstructor
@Slf4j
public class ValidationController {

    private final WorkflowValidationClient validationClient;
    private final WorkflowValidationService validationService;
    private final WorkflowValidatorRepository validatorRepository;

    /**
     * Validates a transition with the given context.
     * POST /api/workflow/validation/transition/{transitionId}
     */
    @PostMapping("/transition/{transitionId}")
    public ResponseEntity<ValidateTransitionResponse> validateTransition(
            @PathVariable UUID transitionId,
            @RequestBody Map<String, Object> contextMap) {

        log.info("Validating transition {} with context", transitionId);

        ValidationResult result = validationClient.validateTransition(transitionId, contextMap);

        List<String> errors = result.allCollect().stream()
                .map(ValidationError::getErrorMessage)
                .collect(Collectors.toList());

        List<String> warnings = result.getWarnings().stream()
                .map(w -> w.getWarningMessage())
                .collect(Collectors.toList());

        ValidateTransitionResponse response = ValidateTransitionResponse.builder()
                .transitionId(transitionId)
                .valid(result.isValid())
                .errors(errors)
                .warnings(warnings)
                .build();

        if (result.isValid()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Quick check if a transition is valid (returns boolean).
     * POST /api/workflow/validation/transition/{transitionId}/check
     */
    @PostMapping("/transition/{transitionId}/check")
    public ResponseEntity<Map<String, Object>> checkTransition(
            @PathVariable UUID transitionId,
            @RequestBody Map<String, Object> contextMap) {

        boolean valid = validationClient.validateTransitionSync(transitionId, contextMap);

        Map<String, Object> response = new HashMap<>();
        response.put("transitionId", transitionId);
        response.put("valid", valid);

        return ResponseEntity.ok(response);
    }

    /**
     * Gets all validators configured for a transition.
     * GET /api/workflow/validation/transition/{transitionId}/validators
     */
    @GetMapping("/transition/{transitionId}/validators")
    public ResponseEntity<List<Map<String, Object>>> getTransitionValidators(
            @PathVariable UUID transitionId) {

        List<WorkflowValidator> validators =
                validatorRepository.findByTransitionIdOrderBySequenceAsc(transitionId);

        List<Map<String, Object>> response = validators.stream()
                .map(this::toValidatorMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Validates a single validator against provided context.
     * Useful for testing validators.
     * POST /api/workflow/validation/validator/{validatorId}
     */
    @PostMapping("/validator/{validatorId}")
    public ResponseEntity<Map<String, Object>> validateSingleValidator(
            @PathVariable UUID validatorId,
            @RequestBody Map<String, Object> contextMap) {

        Optional<WorkflowValidator> validatorOpt = validatorRepository.findById(validatorId);
        if (validatorOpt.isEmpty()) {
            Map<String, Object> error = Map.of(
                    "error", "Validator not found",
                    "validatorId", validatorId.toString());
            return ResponseEntity.notFound().build();
        }

        // Build minimal context
        ValidatorExecutionContext context = buildMinimalContext(contextMap);

        ValidationError error = validationService.validateValidator(validatorOpt.get(), context);

        Map<String, Object> response = new HashMap<>();
        response.put("validatorId", validatorId);
        response.put("valid", error == null);

        if (error != null) {
            response.put("errorMessage", error.getErrorMessage());
            response.put("fieldName", error.getFieldName());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Preview validation - shows what validators are configured without executing them.
     * GET /api/workflow/validation/transition/{transitionId}/preview
     */
    @GetMapping("/transition/{transitionId}/preview")
    public ResponseEntity<Map<String, Object>> previewValidation(@PathVariable UUID transitionId) {

        List<WorkflowValidator> validators =
                validatorRepository.findByTransitionIdOrderBySequenceAsc(transitionId);

        List<Map<String, Object>> validatorPreview = validators.stream()
                .map(v -> {
                    Map<String, Object> preview = new HashMap<>();
                    preview.put("id", v.getId());
                    preview.put("type", v.getValidatorType());
                    preview.put("fieldName", v.getFieldName());
                    preview.put("sequence", v.getSequence());
                    preview.put("continueOnError", v.getContinueOnError());
                    preview.put("hasCustomMessage", v.getErrorMessage() != null && !v.getErrorMessage().isBlank());
                    return preview;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("transitionId", transitionId);
        response.put("validatorCount", validators.size());
        response.put("validators", validatorPreview);

        return ResponseEntity.ok(response);
    }

    /**
     * Bulk validate multiple transitions for an issue.
     * POST /api/workflow/validation/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkValidate(
            @RequestBody BulkValidationRequest request) {

        Map<String, Object> results = new HashMap<>();
        results.put("issueId", request.getIssueId());

        List<Map<String, Object>> transitionResults = new ArrayList<>();

        for (UUID transitionId : request.getTransitionIds()) {
            ValidationResult result = validationClient.validateTransition(
                    transitionId, request.getContext());

            Map<String, Object> transitionResult = new HashMap<>();
            transitionResult.put("transitionId", transitionId);
            transitionResult.put("valid", result.isValid());
            transitionResult.put("errors", result.allCollect().stream()
                    .map(ValidationError::getErrorMessage)
                    .collect(Collectors.toList()));

            transitionResults.add(transitionResult);
        }

        results.put("transitions", transitionResults);
        results.put("totalTransitions", request.getTransitionIds().size());
        results.put("validTransitions", (int) transitionResults.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("valid")))
                .count());

        return ResponseEntity.ok(results);
    }

    // Helper methods

    private Map<String, Object> toValidatorMap(WorkflowValidator v) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", v.getId());
        map.put("transitionId", v.getTransitionId());
        map.put("validatorType", v.getValidatorType());
        map.put("fieldName", v.getFieldName());
        map.put("validatorData", v.getValidatorData());
        map.put("errorMessage", v.getErrorMessage());
        map.put("sequence", v.getSequence());
        map.put("continueOnError", v.getContinueOnError());
        map.put("createdAt", v.getCreatedAt());
        return map;
    }

    private ValidatorExecutionContext buildMinimalContext(Map<String, Object> contextMap) {
        UUID userId = parseUuid(contextMap.get("userId"));
        UUID issueId = parseUuid(contextMap.get("issueId"));
        UUID projectId = parseUuid(contextMap.get("projectId"));
        UUID transitionId = parseUuid(contextMap.get("transitionId"));

        @SuppressWarnings("unchecked")
        Map<String, Object> fields = contextMap.get("fields") instanceof Map
                ? (Map<String, Object>) contextMap.get("fields")
                : Map.of();

        Optional<String> comment = Optional.empty();
        Object commentObj = contextMap.get("comment");
        if (commentObj != null) {
            comment = Optional.of(commentObj.toString());
        }

        return ValidatorExecutionContext.builder()
                .currentUserId(userId)
                .issueId(issueId)
                .projectId(projectId)
                .transitionId(transitionId)
                .issueFields(fields)
                .comment(comment)
                .build();
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // Request DTOs

    @lombok.Data
    public static class BulkValidationRequest {
        private UUID issueId;
        private List<UUID> transitionIds;
        private Map<String, Object> context;
    }
}