package com.jira.migration.workflow.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.workflow.model.WorkflowFunctionDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Maps Jira DC OSWorkflow class descriptors to platform workflow C/V/PF types.
 */
@Component
@RequiredArgsConstructor
public class OsWorkflowDescriptorRegistry {

    private final ObjectMapper objectMapper;

    private static final Map<String, String> VALIDATOR_CLASS_TO_TYPE = Map.ofEntries(
            Map.entry("com.atlassian.jira.workflow.validator.FieldRequiredValidator", "FIELD_REQUIRED"),
            Map.entry("com.atlassian.jira.workflow.validator.CommentRequiredValidator", "COMMENT_REQUIRED"),
            Map.entry("com.atlassian.jira.workflow.validator.PermissionValidator", "USER_PERMISSION"),
            Map.entry("com.opensymphony.workflow.validator.PermissionValidator", "USER_PERMISSION")
    );

    private static final Map<String, String> CONDITION_CLASS_TO_TYPE = Map.ofEntries(
            Map.entry("com.atlassian.jira.workflow.condition.PermissionCondition", "PERMISSION"),
            Map.entry("com.atlassian.jira.workflow.condition.UserInGroupCondition", "USER_GROUP"),
            Map.entry("com.atlassian.jira.workflow.condition.FieldValueCondition", "FIELD_VALUE"),
            Map.entry("com.atlassian.jira.workflow.condition.UserIsReporterCondition", "USER_IS_REPORTER"),
            Map.entry("com.atlassian.jira.workflow.condition.UserIsAssigneeCondition", "USER_IS_ASSIGNEE")
    );

    private static final Map<String, String> POST_FUNCTION_CLASS_TO_TYPE = Map.ofEntries(
            Map.entry("com.atlassian.jira.workflow.function.issue.AssignToCurrentUserFunction", "ASSIGN_TO_CURRENT_USER"),
            Map.entry("com.atlassian.jira.workflow.function.issue.FireIssueEventFunction", "FIRE_EVENT"),
            Map.entry("com.atlassian.jira.workflow.function.issue.UpdateIssueFieldFunction", "UPDATE_ISSUE_FIELD"),
            Map.entry("com.atlassian.jira.workflow.function.issue.GenerateChangeHistoryFunction", "GENERATE_CHANGE_HISTORY"),
            Map.entry("com.atlassian.jira.workflow.function.issue.CreateCommentFunction", "ADD_COMMENT")
    );

    public MappedValidator mapValidator(WorkflowFunctionDescriptor d) {
        String type = VALIDATOR_CLASS_TO_TYPE.getOrDefault(d.getClassName(), "UNSUPPORTED");
        String field = d.getArgs().getOrDefault("fieldKey", d.getArgs().get("field.name"));
        return new MappedValidator(type, field, d.getClassName(), !"UNSUPPORTED".equals(type), toJson(d.getArgs()));
    }

    public MappedCondition mapCondition(WorkflowFunctionDescriptor d) {
        String type = CONDITION_CLASS_TO_TYPE.getOrDefault(d.getClassName(), "UNSUPPORTED");
        String value = d.getArgs().getOrDefault("group", d.getArgs().get("permissionKey"));
        if (value == null) {
            value = d.getArgs().get("fieldValue");
        }
        return new MappedCondition(type, d.getArgs().get("fieldName"), value, d.getClassName(),
                !"UNSUPPORTED".equals(type), toJson(d.getArgs()));
    }

    public MappedPostFunction mapPostFunction(WorkflowFunctionDescriptor d) {
        String type = POST_FUNCTION_CLASS_TO_TYPE.getOrDefault(d.getClassName(), "UNSUPPORTED");
        return new MappedPostFunction(type, d.getClassName(), !"UNSUPPORTED".equals(type), toJson(d.getArgs()));
    }

    public List<String> unsupportedFeatures(WorkflowFunctionDescriptor d, String category) {
        if (d.getClassName() == null) {
            return List.of();
        }
        boolean supported = switch (category) {
            case "validator" -> VALIDATOR_CLASS_TO_TYPE.containsKey(d.getClassName());
            case "condition" -> CONDITION_CLASS_TO_TYPE.containsKey(d.getClassName());
            case "post-function" -> POST_FUNCTION_CLASS_TO_TYPE.containsKey(d.getClassName());
            default -> false;
        };
        return supported ? List.of() : List.of(category + ":" + d.getClassName());
    }

    private String toJson(Map<String, String> args) {
        try {
            return objectMapper.writeValueAsString(args != null ? args : Map.of());
        } catch (Exception e) {
            return "{}";
        }
    }

    public record MappedValidator(String type, String fieldName, String sourceClass, boolean supported, String configJson) {}
    public record MappedCondition(String type, String fieldName, String value, String sourceClass, boolean supported, String configJson) {}
    public record MappedPostFunction(String type, String sourceClass, boolean supported, String configJson) {}
}
