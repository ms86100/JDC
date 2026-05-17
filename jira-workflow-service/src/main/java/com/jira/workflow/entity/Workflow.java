package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Workflow - Enhanced with Jira DC features
 *
 * Workflows define the lifecycle of issues. Each workflow has:
 * - States (statuses) that an issue can be in
 * - Transitions between states
 * - Conditions, validators, and post-functions on transitions
 */
@Entity
@Table(name = "workflows", schema = "jira_workflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_draft", nullable = false)
    @Builder.Default
    private Boolean isDraft = false;

    @Column(name = "draft_of_workflow_id")
    private UUID draftOfWorkflowId;

    @Column(name = "status_category_mapping", columnDefinition = "TEXT")
    private String statusCategoryMapping;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "type", length = 50)
    @Builder.Default
    private String type = "CUSTOM";

    @Column(name = "default_workflow_id")
    private UUID defaultWorkflowId;

    @Column(name = "original_workflow_id")
    private UUID originalWorkflowId;

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

    @Version
    @Column(name = "version")
    private Long version;

    @Transient
    @Builder.Default
    private List<WorkflowStatus> statuses = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<WorkflowTransition> transitions = new ArrayList<>();

    public static final String TYPE_BUILD_IN = "BUILD_IN";
    public static final String TYPE_CUSTOM = "CUSTOM";

    public void lock(UUID userId) {
        this.isLocked = true;
        this.lockedBy = userId;
        this.lockedAt = LocalDateTime.now();
    }

    public void unlock() {
        this.isLocked = false;
        this.lockedBy = null;
        this.lockedAt = null;
    }

    public Workflow createDraft(UUID userId) {
        Workflow draft = Workflow.builder()
                .name(this.name + " (Draft)")
                .description(this.description)
                .isDefault(false)
                .isDraft(true)
                .isActive(true)
                .isSystem(false)
                .draftOfWorkflowId(this.id)
                .statusCategoryMapping(this.statusCategoryMapping)
                .type(this.type)
                .originalWorkflowId(this.id)
                .createdBy(userId)
                .build();
        return draft;
    }

    public void publish() {
        this.isDraft = false;
        this.publishedAt = LocalDateTime.now();
    }
}