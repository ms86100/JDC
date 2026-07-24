package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Automation Rule - event-driven rules that fire independently of workflow transitions.
 * Maps to "Automation for Jira" in Jira Data Center 9.0+.
 */
@Entity
@Table(name = "automation_rules", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRule {

    // Trigger types
    public static final String TRIGGER_ISSUE_CREATED = "ISSUE_CREATED";
    public static final String TRIGGER_ISSUE_UPDATED = "ISSUE_UPDATED";
    public static final String TRIGGER_FIELD_CHANGED = "FIELD_CHANGED";
    public static final String TRIGGER_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String TRIGGER_COMMENT_ADDED = "COMMENT_ADDED";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    // Branch types
    public static final String BRANCH_FOR_EACH_LINKED_ISSUE = "FOR_EACH_LINKED_ISSUE";
    public static final String BRANCH_FOR_EACH_SUBTASK = "FOR_EACH_SUBTASK";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    // -- Trigger --

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    @Column(name = "trigger_config", columnDefinition = "JSONB")
    @Builder.Default
    private String triggerConfig = "{}";

    // -- Conditions --

    @Column(name = "conditions", columnDefinition = "JSONB")
    @Builder.Default
    private String conditions = "[]";

    // -- Actions --

    @Column(name = "actions", nullable = false, columnDefinition = "JSONB")
    @Builder.Default
    private String actions = "[]";

    // -- Branch --

    @Column(name = "branch_type", length = 30)
    private String branchType;

    @Column(name = "branch_link_type", length = 50)
    private String branchLinkType;

    @Column(name = "branch_actions", columnDefinition = "JSONB")
    @Builder.Default
    private String branchActions = "[]";

    // -- Audit --

    @Column(name = "execution_count")
    @Builder.Default
    private Integer executionCount = 0;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
