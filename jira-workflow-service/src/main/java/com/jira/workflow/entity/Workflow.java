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

    @Column(name = "project_id", nullable = false)
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
    private UUID draftOfWorkflowId;  // Reference to original workflow if this is a draft

    @Column(name = "status_category_mapping", columnDefinition = "TEXT")
    private String statusCategoryMapping;  // JSON mapping of status to category

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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

    @Transient
    @Builder.Default
    private List<WorkflowStatus> statuses = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<WorkflowTransition> transitions = new ArrayList<>();

    // Workflow types
    public static final String TYPE_BUILD_IN = "BUILD_IN";
    public static final String TYPE_CUSTOM = "CUSTOM";

    // Copy workflow as draft
    public Workflow createDraft() {
        Workflow draft = Workflow.builder()
                .projectId(this.projectId)
                .name(this.name + " (Draft)")
                .description(this.description)
                .isDefault(false)
                .isDraft(true)
                .draftOfWorkflowId(this.id)
                .statusCategoryMapping(this.statusCategoryMapping)
                .isActive(true)
                .createdBy(this.createdBy)
                .build();
        return draft;
    }

    // Publish draft workflow
    public void publish() {
        this.isDraft = false;
    }
}