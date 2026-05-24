package com.jira.workflow.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.engine.WorkflowIntegrationClient;
import com.jira.workflow.engine.plugin.WorkflowPluginRegistry;
import com.jira.workflow.engine.plugin.WorkflowValidatorProvider;
import com.jira.workflow.entity.WorkflowValidator;
import com.jira.workflow.repository.WorkflowValidatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Workflow Validation Service - Executes validators during workflow transitions.
 * Validates that all conditions are met before allowing a transition to complete.
 * Matches Jira DC's WorkflowValidator engine behavior.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowValidationService {

    private final WorkflowValidatorRepository validatorRepository;
    private final WorkflowIntegrationClient integrationClient;
    private final WorkflowPluginRegistry pluginRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates a list of validators against the given context.
     * This is the main entry point for validation.
     *
     * @param validators List of validators to execute
     * @param context    Execution context with issue data and user info
     * @return ValidationResult with all errors and warnings
     */
    public ValidationResult validateTransition(List<WorkflowValidator> validators, ValidatorExecutionContext context) {
        if (validators == null || validators.isEmpty()) {
            return ValidationResult.success();
        }

        ValidationResult result = ValidationResult.builder()
                .valid(true)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        // Sort validators by sequence
        List<WorkflowValidator> sorted = validators.stream()
                .sorted(Comparator.comparing(WorkflowValidator::getSequence))
                .toList();

        for (WorkflowValidator validator : sorted) {
            ValidationError error = validateValidator(validator, context);
            if (error != null) {
                result.withError(error);
                // If continueOnError is false and we have an error, we continue collecting
                // but callers can use failFast() to get first error
                if (Boolean.FALSE.equals(validator.getContinueOnError())) {
                    log.debug("Validator {} returned error but continueOnError is false, continuing to collect errors",
                            validator.getId());
                }
            }
        }

        return result;
    }

    /**
     * Validates a single validator by dispatching to the appropriate validator method.
     *
     * @param validator The validator to execute
     * @param context  Execution context
     * @return ValidationError if validation failed, null if passed
     */
    public ValidationError validateValidator(WorkflowValidator validator, ValidatorExecutionContext context) {
        if (validator == null) {
            return null;
        }

        String type = validator.getValidatorType();
        if (type == null || type.isBlank()) {
            log.warn("Validator {} has no type, skipping", validator.getId());
            return null;
        }

        return switch (type) {
            case WorkflowValidator.TYPE_FIELD_REQUIRED -> validateFieldRequired(validator, context);
            case WorkflowValidator.TYPE_FIELD_VALUE -> validateFieldValue(validator, context);
            case WorkflowValidator.TYPE_REGEX -> validateRegex(validator, context);
            case WorkflowValidator.TYPE_DATE_RANGE -> validateDateRange(validator, context);
            case WorkflowValidator.TYPE_USER_PERMISSION -> validateUserPermission(validator, context);
            case WorkflowValidator.TYPE_SCRIPT -> validateScript(validator, context);
            case WorkflowValidator.TYPE_COMMENT_REQUIRED -> validateCommentRequired(validator, context);
            case WorkflowValidator.TYPE_ATTACHMENT_COUNT -> validateAttachmentCount(validator, context);
            case WorkflowValidator.TYPE_SUBTASK_RESOLUTION -> validateSubtaskResolution(validator, context);
            case WorkflowValidator.TYPE_LINKED_ISSUE_RESOLUTION -> validateLinkedIssueResolution(validator, context);
            case WorkflowValidator.TYPE_TIME_TRACKING -> validateTimeTracking(validator, context);
            default -> {
                log.warn("Unknown validator type: {} for validator {}", type, validator.getId());
                yield createError(validator, null, "Unknown validator type: " + type);
            }
        };
    }

    /**
     * Validates that a required field is present and not blank.
     * FIELD_REQUIRED validator type.
     */
    public ValidationError validateFieldRequired(WorkflowValidator validator, ValidatorExecutionContext context) {
        String fieldName = validator.getFieldName();
        if (fieldName == null || fieldName.isBlank()) {
            log.warn("FIELD_REQUIRED validator {} has no fieldName", validator.getId());
            return createError(validator, null, "Field name is required for FIELD_REQUIRED validator");
        }

        Object value = context.getFieldValue(fieldName);
        boolean isEmpty = value == null || (value instanceof String str && str.isBlank());

        if (isEmpty) {
            String message = validator.getErrorMessage();
            if (message == null || message.isBlank()) {
                message = "Field '" + fieldName + "' is required";
            }
            return createError(validator, fieldName, message);
        }

        return null;
    }

    /**
     * Validates that a field matches an expected value.
     * FIELD_VALUE validator type.
     */
    public ValidationError validateFieldValue(WorkflowValidator validator, ValidatorExecutionContext context) {
        String fieldName = validator.getFieldName();
        if (fieldName == null || fieldName.isBlank()) {
            return createError(validator, null, "Field name is required for FIELD_VALUE validator");
        }

        String expectedValue = validator.getValidatorData();
        Object actualValue = context.getFieldValue(fieldName);

        // Parse expected value from JSON if needed
        String normalizedExpected = normalizeValue(expectedValue);
        String normalizedActual = normalizeValue(actualValue);

        if (normalizedExpected != null && !normalizedExpected.equalsIgnoreCase(normalizedActual)) {
            String message = validator.getErrorMessage();
            if (message == null || message.isBlank()) {
                message = "Field '" + fieldName + "' must be '" + expectedValue + "'";
            }
            return createError(validator, fieldName, message);
        }

        return null;
    }

    /**
     * Validates that a field value matches a regular expression pattern.
     * REGEX validator type.
     */
    public ValidationError validateRegex(WorkflowValidator validator, ValidatorExecutionContext context) {
        String fieldName = validator.getFieldName();
        String pattern = validator.getValidatorData();

        if (fieldName == null || fieldName.isBlank()) {
            return createError(validator, null, "Field name is required for REGEX validator");
        }

        if (pattern == null || pattern.isBlank()) {
            return createError(validator, fieldName, "Regex pattern is required for REGEX validator");
        }

        Object value = context.getFieldValue(fieldName);
        if (value == null) {
            // Null values pass regex validation (use FIELD_REQUIRED for null checking)
            return null;
        }

        try {
            Pattern regex = Pattern.compile(pattern);
            if (!regex.matcher(value.toString()).matches()) {
                String message = validator.getErrorMessage();
                if (message == null || message.isBlank()) {
                    message = "Field '" + fieldName + "' does not match the required format";
                }
                return createError(validator, fieldName, message);
            }
        } catch (PatternSyntaxException e) {
            log.warn("Invalid regex pattern '{}' in validator {}: {}", pattern, validator.getId(), e.getMessage());
            return createError(validator, fieldName, "Invalid regex pattern configured for field '" + fieldName + "'");
        }

        return null;
    }

    /**
     * Validates that a date field is within a specified range.
     * DATE_RANGE validator type.
     */
    public ValidationError validateDateRange(WorkflowValidator validator, ValidatorExecutionContext context) {
        String fieldName = validator.getFieldName();
        if (fieldName == null || fieldName.isBlank()) {
            return createError(validator, null, "Field name is required for DATE_RANGE validator");
        }

        Object value = context.getFieldValue(fieldName);
        if (value == null) {
            return null; // Let FIELD_REQUIRED handle null check
        }

        // Parse validator data: expected format is JSON with min/max dates
        // {"minDays": 0, "maxDays": 30} or {"minDate": "2024-01-01", "maxDate": "2024-12-31"}
        DateRange range = parseDateRange(validator.getValidatorData());
        if (range == null) {
            return null; // No range configured, skip validation
        }

        LocalDate date;
        try {
            date = parseDate(value.toString());
        } catch (DateTimeParseException e) {
            return createError(validator, fieldName, "Field '" + fieldName + "' is not a valid date");
        }

        // Validate against range
        if (range.minDate != null && date.isBefore(range.minDate)) {
            String message = validator.getErrorMessage();
            if (message == null || message.isBlank()) {
                message = "Field '" + fieldName + "' must be on or after " + range.minDate;
            }
            return createError(validator, fieldName, message);
        }

        if (range.maxDate != null && date.isAfter(range.maxDate)) {
            String message = validator.getErrorMessage();
            if (message == null || message.isBlank()) {
                message = "Field '" + fieldName + "' must be on or before " + range.maxDate;
            }
            return createError(validator, fieldName, message);
        }

        return null;
    }

    /**
     * Validates that the current user has a specific permission.
     * USER_PERMISSION validator type.
     */
    public ValidationError validateUserPermission(WorkflowValidator validator, ValidatorExecutionContext context) {
        String permission = validator.getValidatorData();
        if (permission == null || permission.isBlank()) {
            return createError(validator, null, "Permission name is required for USER_PERMISSION validator");
        }

        if (context.getCurrentUserId() == null) {
            return createError(validator, null, "You do not have permission to perform this transition");
        }

        // Check permission via integration client
        boolean hasPermission = integrationClient.checkUserPermission(
                context.getCurrentUserId(),
                context.getProjectId(),
                permission);

        if (!hasPermission) {
            String message = validator.getErrorMessage();
            if (message == null || message.isBlank()) {
                message = "You do not have the '" + permission + "' permission required for this transition";
            }
            return createError(validator, null, message);
        }

        return null;
    }

    /**
     * Validates using a custom script/rule from the plugin registry.
     * SCRIPT validator type.
     */
    public ValidationError validateScript(WorkflowValidator validator, ValidatorExecutionContext context) {
        String scriptKey = validator.getValidatorData();
        if (scriptKey == null || scriptKey.isBlank()) {
            return createError(validator, null, "Script key is required for SCRIPT validator");
        }

        WorkflowValidatorProvider provider = pluginRegistry.getValidatorProvider(scriptKey);
        if (provider == null) {
            log.warn("No validator provider found for script key: {}", scriptKey);
            return createError(validator, null, "Script validator '" + scriptKey + "' is not available");
        }

        // Build context map for the script
        Map<String, Object> scriptContext = buildScriptContext(context);

        Optional<String> error = provider.validate(scriptContext);
        if (error.isPresent()) {
            String message = validator.getErrorMessage();
            if (message == null || message.isBlank()) {
                message = error.get();
            }
            return createError(validator, null, message);
        }

        return null;
    }

    /**
     * Validates that a comment is present on the transition.
     * COMMENT_REQUIRED validator type.
     */
    public ValidationError validateCommentRequired(WorkflowValidator validator, ValidatorExecutionContext context) {
        if (!context.hasComment()) {
            String message = validator.getErrorMessage();
            if (message == null || message.isBlank()) {
                message = "A comment is required to perform this transition";
            }
            return createError(validator, null, message);
        }

        return null;
    }

    /**
     * Validates that the issue has the required number of attachments.
     * ATTACHMENT_COUNT validator type.
     */
    public ValidationError validateAttachmentCount(WorkflowValidator validator, ValidatorExecutionContext context) {
        int requiredCount = 1; // Default minimum
        String data = validator.getValidatorData();

        if (data != null && !data.isBlank()) {
            try {
                // Try parsing as JSON first: {"min": 1} or {"min": 1, "max": 10}
                Map<String, Object> config = objectMapper.readValue(data, Map.class);
                if (config.containsKey("min")) {
                    requiredCount = ((Number) config.get("min")).intValue();
                }
            } catch (Exception e) {
                // Try parsing as simple integer
                try {
                    requiredCount = Integer.parseInt(data.trim());
                } catch (NumberFormatException ex) {
                    log.warn("Could not parse attachment count config '{}': {}", data, ex.getMessage());
                }
            }
        }

        int actualCount = context.getAttachmentCount();
        if (actualCount < requiredCount) {
            String message = validator.getErrorMessage();
            if (message == null || message.isBlank()) {
                if (requiredCount == 1) {
                    message = "At least one attachment is required for this transition";
                } else {
                    message = "At least " + requiredCount + " attachments are required for this transition";
                }
            }
            return createError(validator, null, message);
        }

        return null;
    }

    /**
     * Validates that all subtasks have the required resolution.
     * SUBTASK_RESOLUTION validator type.
     */
    public ValidationError validateSubtaskResolution(WorkflowValidator validator, ValidatorExecutionContext context) {
        String expectedResolution = validator.getValidatorData();
        if (expectedResolution == null || expectedResolution.isBlank()) {
            return createError(validator, null, "Resolution value is required for SUBTASK_RESOLUTION validator");
        }

        List<ValidatorExecutionContext.Issue> subtasks = context.getSubtasks();
        if (subtasks == null || subtasks.isEmpty()) {
            return null; // No subtasks, validation passes
        }

        for (ValidatorExecutionContext.Issue subtask : subtasks) {
            String actualResolution = subtask.getResolutionName();
            if (actualResolution == null || !actualResolution.equalsIgnoreCase(expectedResolution)) {
                String message = validator.getErrorMessage();
                if (message == null || message.isBlank()) {
                    message = "All subtasks must have resolution '" + expectedResolution + "'";
                    if (subtask.getKey() != null) {
                        message = "Subtask '" + subtask.getKey() + "' must have resolution '" + expectedResolution + "'";
                    }
                }
                return createError(validator, null, message);
            }
        }

        return null;
    }

    /**
     * Validates that linked issues have the required resolution.
     * LINKED_ISSUE_RESOLUTION validator type.
     */
    public ValidationError validateLinkedIssueResolution(WorkflowValidator validator, ValidatorExecutionContext context) {
        String expectedResolution = validator.getValidatorData();
        if (expectedResolution == null || expectedResolution.isBlank()) {
            return createError(validator, null, "Resolution value is required for LINKED_ISSUE_RESOLUTION validator");
        }

        String linkType = validator.getFieldName(); // Use fieldName for link type
        Map<String, ValidatorExecutionContext.Issue> linkedIssues = context.getLinkedIssues();

        if (linkedIssues == null || linkedIssues.isEmpty()) {
            return null; // No linked issues, validation passes
        }

        for (Map.Entry<String, ValidatorExecutionContext.Issue> entry : linkedIssues.entrySet()) {
            // If linkType is specified, only validate that link type
            if (linkType != null && !linkType.isBlank() && !linkType.equalsIgnoreCase(entry.getKey())) {
                continue;
            }

            ValidatorExecutionContext.Issue linkedIssue = entry.getValue();
            String actualResolution = linkedIssue.getResolutionName();

            if (actualResolution == null || !actualResolution.equalsIgnoreCase(expectedResolution)) {
                String message = validator.getErrorMessage();
                if (message == null || message.isBlank()) {
                    if (linkedIssue.getKey() != null) {
                        message = "Linked issue '" + linkedIssue.getKey() + "' must have resolution '" + expectedResolution + "'";
                    } else {
                        message = "All linked issues must have resolution '" + expectedResolution + "'";
                    }
                }
                return createError(validator, null, message);
            }
        }

        return null;
    }

    /**
     * Validates time tracking fields (original/remaining estimate).
     * TIME_TRACKING validator type.
     */
    public ValidationError validateTimeTracking(WorkflowValidator validator, ValidatorExecutionContext context) {
        // Validator data format: JSON with constraints
        // {"minEstimate": 3600, "maxEstimate": 86400} or {"required": true}
        TimeTrackingConfig config = parseTimeTrackingConfig(validator.getValidatorData());
        if (config == null) {
            return null;
        }

        Long originalEstimate = context.getOriginalEstimate();
        Long remainingEstimate = context.getRemainingEstimate();

        // Check if time tracking is required
        if (config.required) {
            if (originalEstimate == null && remainingEstimate == null) {
                String message = validator.getErrorMessage();
                if (message == null || message.isBlank()) {
                    message = "Time tracking is required for this transition";
                }
                return createError(validator, null, message);
            }
        }

        // Validate minimum estimate
        if (config.minEstimate != null) {
            long actual = originalEstimate != null ? originalEstimate : (remainingEstimate != null ? remainingEstimate : 0);
            if (actual < config.minEstimate) {
                String message = validator.getErrorMessage();
                if (message == null || message.isBlank()) {
                    message = "Time estimate must be at least " + formatDuration(config.minEstimate);
                }
                return createError(validator, null, message);
            }
        }

        // Validate maximum estimate
        if (config.maxEstimate != null) {
            long actual = originalEstimate != null ? originalEstimate : (remainingEstimate != null ? remainingEstimate : 0);
            if (actual > config.maxEstimate) {
                String message = validator.getErrorMessage();
                if (message == null || message.isBlank()) {
                    message = "Time estimate must not exceed " + formatDuration(config.maxEstimate);
                }
                return createError(validator, null, message);
            }
        }

        return null;
    }

    // --- Helper Methods ---

    private ValidationError createError(WorkflowValidator validator, String fieldName, String message) {
        return ValidationError.builder()
                .validatorId(validator.getId())
                .fieldName(fieldName)
                .validatorType(validator.getValidatorType())
                .errorMessage(message)
                .build();
    }

    private String normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }

    private LocalDate parseDate(String dateStr) {
        // Try various date formats
        String[] patterns = {
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "MM-dd-yyyy",
                "MM/dd/yyyy",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS"
        };

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
                // Try next pattern
            }
        }

        throw new DateTimeParseException("Could not parse date: " + dateStr, dateStr, 0);
    }

    private DateRange parseDateRange(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(data, Map.class);
            DateRange range = new DateRange();

            if (config.containsKey("minDays")) {
                range.minDate = LocalDate.now().plusDays(-((Number) config.get("minDays")).longValue());
            }
            if (config.containsKey("maxDays")) {
                range.maxDate = LocalDate.now().plusDays(((Number) config.get("maxDays")).longValue());
            }
            if (config.containsKey("minDate")) {
                range.minDate = parseDate(config.get("minDate").toString());
            }
            if (config.containsKey("maxDate")) {
                range.maxDate = parseDate(config.get("maxDate").toString());
            }

            if (range.minDate != null || range.maxDate != null) {
                return range;
            }
        } catch (Exception e) {
            log.warn("Could not parse date range config '{}': {}", data, e.getMessage());
        }

        return null;
    }

    private TimeTrackingConfig parseTimeTrackingConfig(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(data, Map.class);
            TimeTrackingConfig tc = new TimeTrackingConfig();

            if (config.containsKey("required")) {
                tc.required = Boolean.TRUE.equals(config.get("required"));
            }
            if (config.containsKey("minEstimate")) {
                tc.minEstimate = ((Number) config.get("minEstimate")).longValue();
            }
            if (config.containsKey("maxEstimate")) {
                tc.maxEstimate = ((Number) config.get("maxEstimate")).longValue();
            }

            return tc;
        } catch (Exception e) {
            log.warn("Could not parse time tracking config '{}': {}", data, e.getMessage());
        }

        return null;
    }

    private Map<String, Object> buildScriptContext(ValidatorExecutionContext context) {
        Map<String, Object> scriptCtx = new HashMap<>();
        scriptCtx.put("userId", context.getCurrentUserId() != null ? context.getCurrentUserId().toString() : null);
        scriptCtx.put("issueId", context.getIssueId() != null ? context.getIssueId().toString() : null);
        scriptCtx.put("projectId", context.getProjectId() != null ? context.getProjectId().toString() : null);
        scriptCtx.put("transitionId", context.getTransitionId() != null ? context.getTransitionId().toString() : null);
        scriptCtx.put("issueFields", context.getIssueFields());
        scriptCtx.put("comment", context.getCommentOrEmpty());
        scriptCtx.put("originalEstimate", context.getOriginalEstimate());
        scriptCtx.put("remainingEstimate", context.getRemainingEstimate());
        return scriptCtx;
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        }
        if (seconds < 3600) {
            return (seconds / 60) + " minutes";
        }
        if (seconds < 86400) {
            return (seconds / 3600) + " hours";
        }
        return (seconds / 86400) + " days";
    }

    private static class DateRange {
        LocalDate minDate;
        LocalDate maxDate;
    }

    private static class TimeTrackingConfig {
        boolean required = false;
        Long minEstimate;
        Long maxEstimate;
    }
}