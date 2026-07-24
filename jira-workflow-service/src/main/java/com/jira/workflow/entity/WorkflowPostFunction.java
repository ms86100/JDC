package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Workflow Post-Function - Actions executed after a transition completes
 * Matches Jira DC's OFBiz WorkflowAction
 */
@Entity
@Table(name = "workflow_post_functions", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPostFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;

    @Column(name = "function_type", nullable = false, length = 50)
    private String functionType;  // ASSIGN, CREATE_SUBTASK, FIRE_EVENT, UPDATE_FIELD, etc.

    @Column(name = "function_data", columnDefinition = "TEXT")
    private String functionData;  // JSON with function-specific config

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;  // Whether this post-function is active

    @Column(name = "continue_on_error", nullable = false)
    @Builder.Default
    private Boolean continueOnError = false;  // Continue executing other PFs if this one fails

    @Column(name = "async", nullable = false)
    @Builder.Default
    private Boolean async = false;  // Execute asynchronously

    @Column(name = "fail_on_error", nullable = false)
    @Builder.Default
    private Boolean failOnError = true;  // Roll back transition on failure

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Function types - Extended from Jira DC
    public static final String TYPE_ISSUE_ASSIGN = "ISSUE_ASSIGN";
    public static final String TYPE_ISSUE_MOVE = "ISSUE_MOVE";
    public static final String TYPE_NOTIFY_USER = "NOTIFY_USER";
    public static final String TYPE_UPDATE_FIELD = "UPDATE_FIELD";
    public static final String TYPE_ADD_LABEL = "ADD_LABEL";
    public static final String TYPE_REMOVE_LABEL = "REMOVE_LABEL";
    public static final String TYPE_CREATE_SUBTASK = "CREATE_SUBTASK";
    public static final String TYPE_CLONE_ISSUE = "CLONE_ISSUE";
    public static final String TYPE_LINK_ISSUE = "LINK_ISSUE";
    public static final String TYPE_ADD_WATCHER = "ADD_WATCHER";
    public static final String TYPE_REMOVE_WATCHER = "REMOVE_WATCHER";
    public static final String TYPE_FIRE_GLOBAL_EXTENSION = "FIRE_GLOBAL_EXTENSION";
    public static final String TYPE_SET_ISSUE_SECURITY = "SET_ISSUE_SECURITY";
    public static final String TYPE_TRIGGER_AUTOMATION = "TRIGGER_AUTOMATION";
    public static final String TYPE_GENERATE_AUTOMATIC_SUMMARY = "GENERATE_AUTOMATIC_SUMMARY";

    // Legacy Jira DC types (backwards compatibility)
    public static final String TYPE_ASSIGN_TO_CURRENT_USER = "ASSIGN_TO_CURRENT_USER";
    public static final String TYPE_ASSIGN_TO_LAST_USER = "ASSIGN_TO_LAST_USER";
    public static final String TYPE_ASSIGN_TO_PROJECT_LEAD = "ASSIGN_TO_PROJECT_LEAD";
    public static final String TYPE_ASSIGN_TO_REPORTER = "ASSIGN_TO_REPORTER";
    public static final String TYPE_ASSIGN_TO_ROLE = "ASSIGN_TO_ROLE";
    public static final String TYPE_SET_FIELD_VALUE = "SET_FIELD_VALUE";
    public static final String TYPE_COPY_VALUE_FROM_FIELD = "COPY_VALUE_FROM_FIELD";
    public static final String TYPE_SET_ISSUE_STATUS = "SET_ISSUE_STATUS";
    public static final String TYPE_SET_RESOLUTION = "SET_RESOLUTION";
    public static final String TYPE_SET_PRIORITY = "SET_PRIORITY";
    public static final String TYPE_UPDATE_ISSUE_FIELD = "UPDATE_ISSUE_FIELD";
    public static final String TYPE_ADD_COMMENT = "ADD_COMMENT";
    public static final String TYPE_SEND_EMAIL = "SEND_EMAIL";
    public static final String TYPE_FIRE_EVENT = "FIRE_EVENT";
    public static final String TYPE_GENERATE_CHANGE_HISTORY = "GENERATE_CHANGE_HISTORY";
    public static final String TYPE_STORE_ISSUE = "STORE_ISSUE";
    public static final String TYPE_REINDEX_ISSUE = "REINDEX_ISSUE";
    public static final String TYPE_AUTO_TRANSITION = "AUTO_TRANSITION";
    public static final String TYPE_UNLINK_ISSUE = "UNLINK_ISSUE";
    public static final String TYPE_ASSIGN_TO_COMPONENT_LEAD = "ASSIGN_TO_COMPONENT_LEAD";
    public static final String TYPE_SCRIPT_POST_FUNCTION = "SCRIPT_POST_FUNCTION";
    public static final String TYPE_TRIGGER_WEBHOOK = "TRIGGER_WEBHOOK";
}