package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Workflow Condition - Conditions that must be met for a transition to be available
 * Matches Jira DC's OFBiz WorkflowCondition
 */
@Entity
@Table(name = "workflow_conditions", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;

    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;  // PERMISSION, USER_GROUP, FIELD_VALUE, SCRIPT, etc.

    @Column(name = "field_name", length = 100)
    private String fieldName;  // For field-based conditions

    @Column(name = "operator", length = 20)
    private String operator;  // EQUALS, NOT_EQUALS, GREATER_THAN, etc.

    @Column(name = "value", length = 500)
    private String value;  // Expected value

    @Column(name = "condition_data", columnDefinition = "TEXT")
    private String conditionData;  // JSON for complex conditions (e.g., script, groovy expression)

    @Column(name = "negate", nullable = false)
    @Builder.Default
    private Boolean negate = false;  // Invert the condition result

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Condition types
    public static final String TYPE_PERMISSION = "PERMISSION";
    public static final String TYPE_USER_GROUP = "USER_GROUP";
    public static final String TYPE_FIELD_VALUE = "FIELD_VALUE";
    public static final String TYPE_FIELD_CHANGED = "FIELD_CHANGED";
    public static final String TYPE_FIELD_REQUIRED = "FIELD_REQUIRED";
    public static final String TYPE_PREVIOUS_STATUS = "PREVIOUS_STATUS";
    public static final String TYPE_USER_IS_REPORTER = "USER_IS_REPORTER";
    public static final String TYPE_USER_IS_ASSIGNEE = "USER_IS_ASSIGNEE";
    public static final String TYPE_USER_IS_CURRENT_USER = "USER_IS_CURRENT_USER";
    public static final String TYPE_SPRINT_STATUS = "SPRINT_STATUS";
    public static final String TYPE_SUBTASK_STATUS = "SUBTASK_STATUS";
    public static final String TYPE_LINKED_ISSUE_STATUS = "LINKED_ISSUE_STATUS";
    public static final String TYPE_SCRIPT = "SCRIPT";
    public static final String TYPE_AND = "AND";
    public static final String TYPE_OR = "OR";
    public static final String TYPE_NOT = "NOT";

    // Operators
    public static final String OP_EQUALS = "EQUALS";
    public static final String OP_NOT_EQUALS = "NOT_EQUALS";
    public static final String OP_GREATER_THAN = "GREATER_THAN";
    public static final String OP_LESS_THAN = "LESS_THAN";
    public static final String OP_CONTAINS = "CONTAINS";
    public static final String OP_NOT_CONTAINS = "NOT_CONTAINS";
    public static final String OP_IN = "IN";
    public static final String OP_NOT_IN = "NOT_IN";
    public static final String OP_IS_EMPTY = "IS_EMPTY";
    public static final String OP_IS_NOT_EMPTY = "IS_NOT_EMPTY";
}