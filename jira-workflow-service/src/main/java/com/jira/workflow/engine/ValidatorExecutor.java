package com.jira.workflow.engine;

import com.jira.workflow.engine.plugin.WorkflowPluginRegistry;
import com.jira.workflow.entity.WorkflowValidator;
import com.jira.workflow.repository.WorkflowValidatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidatorExecutor {

    private static final Set<String> SUPPORTED_TYPES = Set.copyOf(new LinkedHashSet<>(List.of(
            "REQUIRED_FIELD",
            WorkflowValidator.TYPE_FIELD_REQUIRED,
            "RESOLUTION_REQUIRED",
            WorkflowValidator.TYPE_COMMENT_REQUIRED,
            WorkflowValidator.TYPE_REGEX,
            "PARENT_STATUS",
            "ATTACHMENT_REQUIRED",
            WorkflowValidator.TYPE_ATTACHMENT_COUNT,
            WorkflowValidator.TYPE_SCRIPT
    )));

    private final WorkflowValidatorRepository workflowValidatorRepository;
    private final TransitionScreenService transitionScreenService;
    private final WorkflowIntegrationClient integrationClient;
    private final WorkflowPluginRegistry pluginRegistry;

    public ValidationResult validate(UUID transitionId, WorkflowContext ctx) {
        Map<String, String> fieldErrors = new LinkedHashMap<>(
                transitionScreenService.validateScreenInputFields(
                        ctx.getTransition(), ctx.getScreenInput(), ctx.getIssueData()));

        List<String> errors = new ArrayList<>(fieldErrors.values());

        List<WorkflowValidator> validators = workflowValidatorRepository.findByTransitionIdOrderBySequenceAsc(transitionId);
        for (WorkflowValidator validator : validators) {
            String err = evaluate(validator, ctx);
            if (err != null) {
                errors.add(err);
                if (validator.getFieldName() != null) {
                    fieldErrors.putIfAbsent(validator.getFieldName(), err);
                }
            }
        }
        return new ValidationResult(errors, fieldErrors);
    }

    private String evaluate(WorkflowValidator validator, WorkflowContext ctx) {
        String type = validator.getValidatorType();
        if (type != null && !SUPPORTED_TYPES.contains(type)) {
            log.debug("Skipping unsupported validator type: {}", type);
            return null;
        }

        Map<String, Object> issue = ctx.getIssueData();
        Map<String, Object> screen = ctx.getScreenInput() != null ? ctx.getScreenInput() : Map.of();
        String customMessage = validator.getErrorMessage();

        if ("REQUIRED_FIELD".equals(type) || WorkflowValidator.TYPE_FIELD_REQUIRED.equals(type)) {
            String field = validator.getFieldName();
            Object val = screen.containsKey(field) ? screen.get(field) : issue.get(field);
            if (val == null || val.toString().isBlank()) {
                return customMessage != null ? customMessage : "Required field: " + field;
            }
            return null;
        }
        if ("RESOLUTION_REQUIRED".equals(type)) {
            Object res = screen.get("resolutionId");
            if (res == null) res = ctx.getResolutionId();
            if (res == null) res = issue.get("resolutionId");
            return res == null
                    ? (customMessage != null ? customMessage : "Resolution is required for this transition")
                    : null;
        }
        if ("COMMENT_REQUIRED".equals(type) || WorkflowValidator.TYPE_COMMENT_REQUIRED.equals(type)) {
            String comment = ctx.getComment();
            if (comment == null || comment.isBlank()) {
                comment = screen.get("comment") != null ? String.valueOf(screen.get("comment")) : null;
            }
            return (comment == null || comment.isBlank())
                    ? (customMessage != null ? customMessage : "Comment is required")
                    : null;
        }
        if ("REGEX".equals(type) || WorkflowValidator.TYPE_REGEX.equals(type)) {
            String field = validator.getFieldName();
            Object val = screen.getOrDefault(field, issue.get(field));
            if (val == null) return null;
            String pattern = validator.getValidatorData();
            if (pattern != null && !val.toString().matches(pattern)) {
                return customMessage != null ? customMessage : "Field " + field + " does not match required format";
            }
            return null;
        }
        if ("PARENT_STATUS".equals(type)) {
            Object parentStatus = issue.get("parentStatus");
            String expected = validator.getValidatorData();
            if (expected != null && parentStatus != null && !expected.equalsIgnoreCase(parentStatus.toString())) {
                return customMessage != null ? customMessage : "Parent issue must be in status: " + expected;
            }
            return null;
        }
        if ("ATTACHMENT_REQUIRED".equals(type) || WorkflowValidator.TYPE_ATTACHMENT_COUNT.equals(type)) {
            int min = 1;
            if (validator.getValidatorData() != null && !validator.getValidatorData().isBlank()) {
                try {
                    min = Integer.parseInt(validator.getValidatorData().trim());
                } catch (NumberFormatException ignored) {
                    min = 1;
                }
            }
            int count = integrationClient.countAttachments(ctx.getIssueId());
            if (count < min) {
                return customMessage != null ? customMessage : "At least " + min + " attachment(s) required";
            }
            return null;
        }
        if (WorkflowValidator.TYPE_SCRIPT.equals(type)) {
            String scriptKey = validator.getValidatorData();
            if (scriptKey == null || scriptKey.isBlank()) {
                return customMessage != null ? customMessage : "Script validator not configured";
            }
            Map<String, Object> pluginCtx = buildEnrichedContext(ctx);
            Optional<String> validationError = pluginRegistry.validateWithProvider(scriptKey, pluginCtx);
            if (validationError.isPresent()) {
                return customMessage != null ? customMessage : validationError.get();
            }
            return null;
        }
        return null;
    }

    private Map<String, Object> buildEnrichedContext(WorkflowContext ctx) {
        Map<String, Object> pluginCtx = new HashMap<>();
        pluginCtx.put("issueId", ctx.getIssueId() != null ? ctx.getIssueId().toString() : null);
        pluginCtx.put("projectId", ctx.getProjectId() != null ? ctx.getProjectId().toString() : null);
        pluginCtx.put("userId", ctx.getUserId() != null ? ctx.getUserId().toString() : null);
        pluginCtx.put("issueTypeId", ctx.getIssueTypeId() != null ? ctx.getIssueTypeId().toString() : null);
        pluginCtx.put("currentStatusId", ctx.getCurrentStatusId() != null ? ctx.getCurrentStatusId().toString() : null);
        pluginCtx.put("transitionId", ctx.getTransition() != null ? ctx.getTransition().getId().toString() : null);
        pluginCtx.put("transitionName", ctx.getTransition() != null ? ctx.getTransition().getName() : null);
        pluginCtx.put("fromStatusId", ctx.getCurrentStatusId() != null ? ctx.getCurrentStatusId().toString() : null);
        pluginCtx.put("toStatusId", ctx.getTransition() != null ? ctx.getTransition().getToStatusId().toString() : null);
        pluginCtx.put("issueData", ctx.getIssueData() != null ? ctx.getIssueData() : Map.of());
        pluginCtx.put("userData", ctx.getUserData() != null ? ctx.getUserData() : Map.of());
        pluginCtx.put("screenInput", ctx.getScreenInput() != null ? ctx.getScreenInput() : Map.of());
        pluginCtx.put("comment", ctx.getComment());
        pluginCtx.put("resolutionId", ctx.getResolutionId() != null ? ctx.getResolutionId().toString() : null);
        return pluginCtx;
    }

    public record ValidationResult(List<String> errors, Map<String, String> fieldErrors) {
        public boolean isEmpty() {
            return errors == null || errors.isEmpty();
        }
    }
}
