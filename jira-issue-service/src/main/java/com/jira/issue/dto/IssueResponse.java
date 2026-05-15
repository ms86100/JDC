package com.jira.issue.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponse {

    private UUID id;
    private UUID projectId;
    private String issueKey;
    private String title;
    private String description;
    private UUID statusId;
    private String statusName;
    private String statusCategory;
    private UUID priorityId;
    private String priorityName;
    private String priorityColor;
    private UUID issueTypeId;
    private String issueTypeName;
    private String issueTypeIcon;
    private UUID reporterId;
    private UUID assigneeId;
    private UUID parentIssueId;
    private String parentIssueKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Epic fields
    private UUID epicId;
    private String epicName;
    private String epicColor;

    // Security level
    private UUID securityLevelId;
    private String securityLevelName;

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
    private String resolutionName;
    private LocalDateTime resolutionDate;

    // Due date
    private LocalDate dueDate;

    // Labels
    private String[] labels;

    // Components
    private UUID[] componentIds;

    // Votes and watchers
    private Integer voteCount;
    private Integer watcherCount;

    // Sub-task indicator
    private Boolean subTask;
    private Integer subTaskCount;
}
