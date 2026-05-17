package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_transition_triggers", schema = "jira_workflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private String triggerConfig;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "execution_order")
    @Builder.Default
    private Integer executionOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_AUTOMATIC = "AUTOMATIC";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_WEBHOOK = "WEBHOOK";
    public static final String TRIGGER_FIELD_CHANGE = "FIELD_CHANGE";
    public static final String TRIGGER_ISSUE_LINK = "ISSUE_LINK";
}