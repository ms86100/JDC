package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Workflow Validator - Validators that check if a transition can complete
 * Matches Jira DC's OFBiz WorkflowValidator
 */
@Entity
@Table(name = "workflow_validators", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowValidator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;

    @Column(name = "validator_type", nullable = false, length = 50)
    private String validatorType;  // FIELD_REQUIRED, REGEX, SCRIPT, DATE_RANGE, etc.

    @Column(name = "field_name", length = 100)
    private String fieldName;  // Field to validate

    @Column(name = "validator_data", columnDefinition = "TEXT")
    private String validatorData;  // JSON with validator-specific config

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;  // Message to show on validation failure

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "continue_on_error", nullable = false)
    @Builder.Default
    private Boolean continueOnError = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Validator types
    public static final String TYPE_FIELD_REQUIRED = "FIELD_REQUIRED";
    public static final String TYPE_FIELD_VALUE = "FIELD_VALUE";
    public static final String TYPE_REGEX = "REGEX";
    public static final String TYPE_DATE_RANGE = "DATE_RANGE";
    public static final String TYPE_USER_PERMISSION = "USER_PERMISSION";
    public static final String TYPE_SCRIPT = "SCRIPT";
    public static final String TYPE_SUBTASK_RESOLUTION = "SUBTASK_RESOLUTION";
    public static final String TYPE_LINKED_ISSUE_RESOLUTION = "LINKED_ISSUE_RESOLUTION";
    public static final String TYPE_ATTACHMENT_COUNT = "ATTACHMENT_COUNT";
    public static final String TYPE_COMMENT_REQUIRED = "COMMENT_REQUIRED";
    public static final String TYPE_TIME_TRACKING = "TIME_TRACKING";
}