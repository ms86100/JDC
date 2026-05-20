package com.jira.workflow.engine;

import com.jira.workflow.entity.WorkflowValidator;
import com.jira.workflow.repository.WorkflowValidatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidatorExecutor {

    private final WorkflowValidatorRepository workflowValidatorRepository;
    private final TransitionScreenService transitionScreenService;

    public List<String> validate(UUID transitionId, WorkflowContext ctx) {
        List<String> errors = new ArrayList<>();
        errors.addAll(transitionScreenService.validateScreenInput(ctx.getTransition(), ctx.getScreenInput(), ctx.getIssueData()));

        List<WorkflowValidator> validators = workflowValidatorRepository.findByTransitionIdOrderBySequenceAsc(transitionId);
        for (WorkflowValidator validator : validators) {
            String err = evaluate(validator, ctx);
            if (err != null) {
                errors.add(err);
            }
        }
        return errors;
    }

    private String evaluate(WorkflowValidator validator, WorkflowContext ctx) {
        String type = validator.getValidatorType();
        Map<String, Object> issue = ctx.getIssueData();
        Map<String, Object> screen = ctx.getScreenInput() != null ? ctx.getScreenInput() : Map.of();

        if ("REQUIRED_FIELD".equals(type) || WorkflowValidator.TYPE_FIELD_REQUIRED.equals(type)) {
            String field = validator.getFieldName();
            Object val = screen.containsKey(field) ? screen.get(field) : issue.get(field);
            return (val == null || val.toString().isBlank()) ? "Required field: " + field : null;
        }
        if ("RESOLUTION_REQUIRED".equals(type)) {
            Object res = screen.get("resolutionId");
            if (res == null) res = ctx.getResolutionId();
            if (res == null) res = issue.get("resolutionId");
            return res == null ? "Resolution is required for this transition" : null;
        }
        if ("COMMENT_REQUIRED".equals(type) || WorkflowValidator.TYPE_COMMENT_REQUIRED.equals(type)) {
            String comment = ctx.getComment();
            if (comment == null || comment.isBlank()) {
                comment = screen.get("comment") != null ? String.valueOf(screen.get("comment")) : null;
            }
            return (comment == null || comment.isBlank()) ? "Comment is required" : null;
        }
        if ("REGEX".equals(type) || WorkflowValidator.TYPE_REGEX.equals(type)) {
            String field = validator.getFieldName();
            Object val = screen.getOrDefault(field, issue.get(field));
            if (val == null) return null;
            String pattern = validator.getValidatorData();
            return (pattern != null && !val.toString().matches(pattern))
                    ? "Field " + field + " does not match required format" : null;
        }
        if ("PARENT_STATUS".equals(type)) {
            Object parentStatus = issue.get("parentStatus");
            String expected = validator.getValidatorData();
            return (expected != null && parentStatus != null && !expected.equalsIgnoreCase(parentStatus.toString()))
                    ? "Parent issue must be in status: " + expected : null;
        }
        log.warn("Unknown validator {}, blocking", type);
        return "Unknown validator: " + type;
    }
}
