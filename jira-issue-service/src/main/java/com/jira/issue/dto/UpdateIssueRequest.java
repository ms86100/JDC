package com.jira.issue.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueRequest {

    private String title;
    private String description;

    // Assignee and priority
    private UUID assigneeId;
    private UUID priorityId;
    private UUID issueTypeId;

    // Epic fields
    private UUID epicId;
    private String epicName;
    private String epicColor;

    // Security level
    private UUID securityLevelId;

    // Parent issue (for sub-tasks)
    private UUID parentIssueId;

    // Versions
    private UUID[] affectsVersions;
    private UUID[] fixVersions;

    // Story points and rank
    private Integer storyPoints;
    private String rank;

    // Time tracking (in seconds)
    private Long originalEstimate;
    private Long remainingEstimate;
    private Long timeSpent;

    // Resolution
    private UUID resolutionId;
    private LocalDateTime resolutionDate;

    // Due date
    private LocalDate dueDate;

    // Labels
    private String[] labels;

    // Components
    private UUID[] componentIds;

    // Status (for workflow transitions)
    private UUID statusId;
}