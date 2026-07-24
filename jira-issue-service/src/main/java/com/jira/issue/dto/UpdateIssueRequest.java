package com.jira.issue.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueRequest {

    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    private String description;  // No length limit for description (may be rich text)

    private String environment;

    // Assignee and priority
    private UUID assigneeId;
    private UUID priorityId;
    private UUID issueTypeId;

    // Epic fields
    private UUID epicId;

    @Size(max = 255, message = "Epic name must not exceed 255 characters")
    private String epicName;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Epic color must be a valid hex color (e.g., #FF5733)")
    private String epicColor;

    // Security level
    private UUID securityLevelId;

    // Parent issue (for sub-tasks)
    private UUID parentIssueId;

    // Versions
    private UUID[] affectsVersions;
    private UUID[] fixVersions;

    // Story points and rank
    @Min(value = 0, message = "Story points cannot be negative")
    @Max(value = 10000, message = "Story points cannot exceed 10000")
    private Integer storyPoints;

    private Integer businessValue;

    private String rank;

    // Time tracking (in seconds)
    @Min(value = 0, message = "Original estimate cannot be negative")
    private Long originalEstimate;

    @Min(value = 0, message = "Remaining estimate cannot be negative")
    private Long remainingEstimate;

    @Min(value = 0, message = "Time spent cannot be negative")
    private Long timeSpent;

    // Resolution
    private UUID resolutionId;
    private LocalDateTime resolutionDate;

    // Due date (no @FutureOrPresent — allow existing/backdated issues on edit)
    private LocalDate dueDate;

    // Labels
    private String[] labels;

    // Components
    private UUID[] componentIds;

    // Status (for workflow transitions)
    private UUID statusId;

    // Optimistic locking - required for updates to detect concurrent modifications
    @Min(value = 0, message = "Version must be non-negative")
    private Long expectedVersion;
}