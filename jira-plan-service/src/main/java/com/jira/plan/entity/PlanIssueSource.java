package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_issue_sources", schema = "jira_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanIssueSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "source_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "source_name", nullable = false, length = 255)
    private String sourceName;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "issue_count")
    @Builder.Default
    private Integer issueCount = 0;

    @Column(name = "sync_error")
    private String syncError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum SourceType {
        BOARD,
        PROJECT,
        FILTER
    }

    public void markSyncSuccess(int issueCount) {
        this.lastSyncAt = LocalDateTime.now();
        this.issueCount = issueCount;
        this.syncError = null;
    }

    public void markSyncError(String error) {
        this.lastSyncAt = LocalDateTime.now();
        this.syncError = error;
    }
}