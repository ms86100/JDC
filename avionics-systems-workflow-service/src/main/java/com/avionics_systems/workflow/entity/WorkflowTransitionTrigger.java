package com.avionics_systems.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_transition_triggers", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private String triggerConfig;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "execution_order")
    @Builder.Default
    private Integer executionOrder = 0;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count")
    @Builder.Default
    private Integer triggerCount = 0;

    @Column(name = "cooldown_seconds")
    @Builder.Default
    private Integer cooldownSeconds = 60;

    @Column(name = "max_fire_count")
    @Builder.Default
    private Integer maxFireCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private String conditions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Trigger type constants
    public static final String TRIGGER_TYPE_FIELD_CHANGE = "FIELD_CHANGE";
    public static final String TRIGGER_TYPE_COMMENT_ADDED = "COMMENT_ADDED";
    public static final String TRIGGER_TYPE_ATTACHMENT_ADDED = "ATTACHMENT_ADDED";
    public static final String TRIGGER_TYPE_LINK_ADDED = "LINK_ADDED";
    public static final String TRIGGER_TYPE_STATUS_CHANGE = "STATUS_CHANGE";
    public static final String TRIGGER_TYPE_DATE_BASED = "DATE_BASED";
    public static final String TRIGGER_TYPE_EXTERNAL_WEBHOOK = "EXTERNAL_WEBHOOK";
    public static final String TRIGGER_TYPE_API_TRIGGER = "API_TRIGGER";
    public static final String TRIGGER_TYPE_SPRINT_START = "SPRINT_START";
    public static final String TRIGGER_TYPE_SPRINT_COMPLETE = "SPRINT_COMPLETE";
    public static final String TRIGGER_TYPE_BUILD_SUCCESS = "BUILD_SUCCESS";
    public static final String TRIGGER_TYPE_PULL_REQUEST = "PULL_REQUEST";
    public static final String TRIGGER_TYPE_MANUAL = "MANUAL";
    public static final String TRIGGER_TYPE_AUTOMATIC = "AUTOMATIC";
    public static final String TRIGGER_TYPE_SCHEDULED = "SCHEDULED";

    // Event type constants
    public static final String EVENT_ISSUE_UPDATED = "ISSUE_UPDATED";
    public static final String EVENT_FIELD_CHANGED = "FIELD_CHANGED";
    public static final String EVENT_COMMENT_ADDED = "COMMENT_ADDED";
    public static final String EVENT_ATTACHMENT_ADDED = "ATTACHMENT_ADDED";
    public static final String EVENT_LINK_CREATED = "LINK_CREATED";
    public static final String EVENT_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String EVENT_DATE_REACHED = "DATE_REACHED";
    public static final String EVENT_API_CALL = "API_CALL";
    public static final String EVENT_SPRINT_STARTED = "SPRINT_STARTED";
    public static final String EVENT_SPRINT_COMPLETED = "SPRINT_COMPLETED";
}