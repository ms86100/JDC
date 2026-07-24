package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dcl_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DclMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "action_responsible", length = 255)
    private String actionResponsible;

    @Column(name = "requested_by", length = 255)
    private String requestedBy;

    @Column(name = "dcl_abstract", columnDefinition = "TEXT")
    private String dclAbstract;

    @Column(name = "description_thales", columnDefinition = "TEXT")
    private String descriptionThales;

    @Column(name = "description_honeywell", columnDefinition = "TEXT")
    private String descriptionHoneywell;

    @Column(name = "supplier_sync_project_id")
    private UUID supplierSyncProjectId;

    @Column(name = "supplier_sync_issue_id")
    private UUID supplierSyncIssueId;

    @Column(name = "sync_direction", length = 30)
    private String syncDirection;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
