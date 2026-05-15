package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "issues", schema = "jira_issue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "issue_key", nullable = false, unique = true, length = 20)
    private String issueKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status", nullable = false)
    private IssueStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "priority")
    private IssuePriority priority;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "issue_type", nullable = false)
    private IssueType issueType;

    @Column(name = "reporter_id")
    private UUID reporterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    // Epic/Story hierarchy
    @Column(name = "epic_id")
    private UUID epicId;

    @Column(name = "epic_name", length = 255)
    private String epicName;

    @Column(name = "epic_color", length = 7)
    private String epicColor;

    @Column(name = "parent_issue_id")
    private UUID parentIssueId;

    // Security level
    @Column(name = "security_level_id")
    private UUID securityLevelId;

    // Versions (affects/fixes)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "affects_versions", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] affectsVersions = new UUID[]{};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "fix_versions", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] fixVersions = new UUID[]{};

    // Story points and rank
    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(length = 255)
    private String rank;

    // Time tracking (in seconds)
    @Column(name = "original_estimate")
    private Long originalEstimate;

    @Column(name = "remaining_estimate")
    private Long remainingEstimate;

    @Column(name = "time_spent")
    private Long timeSpent;

    // Resolution
    @Column(name = "resolution_id")
    private UUID resolutionId;

    @Column(name = "resolution_date")
    private LocalDateTime resolutionDate;

    // Due date
    @Column(name = "due_date")
    private LocalDate dueDate;

    // Votes and watchers count (denormalized)
    @Column(name = "vote_count")
    @Builder.Default
    private Integer voteCount = 0;

    @Column(name = "watcher_count")
    @Builder.Default
    private Integer watcherCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public void incrementVoteCount() {
        this.voteCount = (this.voteCount == null ? 0 : this.voteCount) + 1;
    }

    public void decrementVoteCount() {
        this.voteCount = (this.voteCount == null ? 0 : this.voteCount) - 1;
    }

    public void incrementWatcherCount() {
        this.watcherCount = (this.watcherCount == null ? 0 : this.watcherCount) + 1;
    }

    public void decrementWatcherCount() {
        this.watcherCount = (this.watcherCount == null ? 0 : this.watcherCount) - 1;
    }
}