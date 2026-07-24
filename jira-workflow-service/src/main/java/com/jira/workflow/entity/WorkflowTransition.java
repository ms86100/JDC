package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Workflow Transition - Enhanced with Jira DC features
 *
 * Represents a transition between workflow statuses with:
 * - Conditions (who can perform the transition)
 * - Validators (validation before transition completes)
 * - Post-functions (actions performed after transition)
 */
@Entity
@Table(name = "workflow_transitions", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "from_status_id", nullable = false)
    private UUID fromStatusId;

    @Column(name = "to_status_id", nullable = false)
    private UUID toStatusId;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "type", length = 50)
    @Builder.Default
    private String type = "MANUAL";

    @Column(name = "trigger_type", length = 50)
    private String triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private Map<String, Object> triggerConfig;

    @Column(name = "origin", length = 50)
    @Builder.Default
    private String origin = "USER";

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private Boolean requiresApproval = false;

    @Column(name = "approval_group_id")
    private UUID approvalGroupId;

    @Column(name = "allow_assignee_override", nullable = false)
    @Builder.Default
    private Boolean allowAssigneeOverride = true;

    @Column(name = "allow_unassign", nullable = false)
    @Builder.Default
    private Boolean allowUnassign = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields_required", columnDefinition = "jsonb")
    private List<String> fieldsRequired;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields_updated", columnDefinition = "jsonb")
    private List<Map<String, Object>> fieldsUpdated;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields_hidden", columnDefinition = "jsonb")
    private List<String> fieldsHidden;

    @Column(name = "fields_auto_submit", nullable = false)
    @Builder.Default
    private Boolean fieldsAutoSubmit = false;

    @Column(name = "permission_check", length = 50)
    private String permissionCheck;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_group_ids", columnDefinition = "jsonb")
    private List<String> userGroupIds;

    @Column(name = "remote_link_transition", nullable = false)
    @Builder.Default
    private Boolean remoteLinkTransition = false;

    @Column(name = "remote_link_direction", length = 10)
    private String remoteLinkDirection;

    @Column(name = "remote_link_issue_link_type", length = 50)
    private String remoteLinkIssueLinkType;

    @Column(name = "allow_loop", nullable = false)
    @Builder.Default
    private Boolean allowLoop = false;

    @Column(name = "max_loop_count")
    @Builder.Default
    private Integer maxLoopCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_conditions", columnDefinition = "jsonb")
    private List<Map<String, Object>> conditionConditions;

    @Column(name = "condition_operator", length = 10)
    @Builder.Default
    private String conditionOperator = "AND";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validator_validators", columnDefinition = "jsonb")
    private List<Map<String, Object>> validatorValidators;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "post_function_functions", columnDefinition = "jsonb")
    private List<Map<String, Object>> postFunctionFunctions;

    @Column(name = "screen_id")
    private UUID screenId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    public static final String OPERATION_AUTO = "AUTO";
    public static final String OPERATION_MANUAL = "MANUAL";
    public static final String OPERATION_SCRIPT = "SCRIPT";

    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_AUTOMATIC = "AUTOMATIC";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_WEBHOOK = "WEBHOOK";

    public boolean hasCondition() {
        return permissionCheck != null || (userGroupIds != null && !userGroupIds.isEmpty());
    }

    public boolean requiresFields() {
        return fieldsRequired != null && !fieldsRequired.isEmpty();
    }

    // JSON serialization helpers for conditions, validators, and post-functions
    public String getConditions() {
        if (conditionConditions == null) return "[]";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(conditionConditions);
        } catch (Exception e) {
            return "[]";
        }
    }

    public void setConditions(String conditions) {
        if (conditions == null || conditions.isEmpty() || conditions.equals("[]")) {
            this.conditionConditions = new ArrayList<>();
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.conditionConditions = mapper.readValue(conditions,
                mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            this.conditionConditions = new ArrayList<>();
        }
    }

    public String getValidators() {
        if (validatorValidators == null) return "[]";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(validatorValidators);
        } catch (Exception e) {
            return "[]";
        }
    }

    public void setValidators(String validators) {
        if (validators == null || validators.isEmpty() || validators.equals("[]")) {
            this.validatorValidators = new ArrayList<>();
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.validatorValidators = mapper.readValue(validators,
                mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            this.validatorValidators = new ArrayList<>();
        }
    }

    public String getPostFunctions() {
        if (postFunctionFunctions == null) return "[]";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(postFunctionFunctions);
        } catch (Exception e) {
            return "[]";
        }
    }

    public void setPostFunctions(String postFunctions) {
        if (postFunctions == null || postFunctions.isEmpty() || postFunctions.equals("[]")) {
            this.postFunctionFunctions = new ArrayList<>();
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.postFunctionFunctions = mapper.readValue(postFunctions,
                mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            this.postFunctionFunctions = new ArrayList<>();
        }
    }
}