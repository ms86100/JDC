package com.jira.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIssueRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    private String description;

    private UUID issueTypeId;

    private UUID priorityId;

    private UUID assigneeId;

    private UUID reporterId;

    private UUID parentIssueId;

    // Epic fields
    private UUID epicId;
    private String epicName;
    private String epicColor;

    // Security level
    private UUID securityLevelId;

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

    // Due date
    private LocalDate dueDate;

    // Labels
    private String[] labels;

    // Components
    private UUID[] componentIds;

    // Migration support: preserve original issue key from source system
    private String issueKey;

    // Migration support: preserve original timestamps
    private String migrationCreatedAt;
    private String migrationUpdatedAt;
}