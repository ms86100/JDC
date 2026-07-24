package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_drafts", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "draft_data", columnDefinition = "jsonb", nullable = false)
    private String draftData;

    @Column(name = "parent_version")
    private Integer parentVersion;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_draft_of_published")
    @Builder.Default
    private Boolean isDraftOfPublished = false;

    @Column(name = "draft_status", length = 20)
    @Builder.Default
    private String draftStatus = "ACTIVE";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_DISCARDED = "DISCARDED";
}