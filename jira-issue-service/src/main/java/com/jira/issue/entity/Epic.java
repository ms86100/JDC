package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "epics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Epic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 7)
    @Builder.Default
    private String color = "#0052CC";

    @Column(name = "lead_id")
    private String leadId;

    @Column(name = "lead_name")
    private String leadName;

    @Column(length = 50)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "linked_issue_id")
    private String linkedIssueId;

    @Column(name = "total_story_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalStoryPoints = BigDecimal.ZERO;

    @Column(name = "completed_story_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal completedStoryPoints = BigDecimal.ZERO;

    @Column(name = "total_issue_count")
    @Builder.Default
    private Integer totalIssueCount = 0;

    @Column(name = "completed_issue_count")
    @Builder.Default
    private Integer completedIssueCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum EpicStatus {
        OPEN, IN_PROGRESS, COMPLETE
    }

    public void updateProgress(BigDecimal totalPoints, BigDecimal completedPoints, int totalIssues, int completedIssues) {
        this.totalStoryPoints = totalPoints;
        this.completedStoryPoints = completedPoints;
        this.totalIssueCount = totalIssues;
        this.completedIssueCount = completedIssues;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getProgressPercentage() {
        if (totalStoryPoints == null || totalStoryPoints.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return completedStoryPoints.multiply(BigDecimal.valueOf(100))
                .divide(totalStoryPoints, 2, java.math.RoundingMode.HALF_UP);
    }
}