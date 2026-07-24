package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintReportResponse {
    private UUID sprintId;
    private String sprintName;
    private String sprintGoal;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate completeDate;
    private String status;

    // Issue metrics
    private Integer totalIssues;
    private Integer completedIssues;
    private Integer inProgressIssues;
    private Integer todoIssues;
    private Integer blockedIssues;

    // Point metrics
    private Integer totalPoints;
    private Integer completedPoints;
    private Integer remainingPoints;
    private Double completionRate;
    private Double pointsCompletionRate;

    // Time metrics
    private Integer daysRemaining;
    private Double dailyBurnRate;
    private Integer projectedCompletion;

    // Work distribution
    private Map<String, Integer> issuesByStatus;
    private Map<String, Integer> issuesByPriority;
    private Map<String, Integer> issuesByType;
    private Map<String, Integer> issuesByAssignee;

    // Velocity data
    private BurndownResponse burndown;
    private VelocityResponse velocity;

    // Scope change tracking
    private List<String> issuesAddedDuringSprint;
    private List<String> issuesRemovedDuringSprint;
    private List<String> issuesNotCompleted;
}