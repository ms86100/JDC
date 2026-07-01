package com.jira.issue.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate releaseDate;
    private Boolean isReleased;
    private Boolean isArchived;
    private Integer sortOrder;
    private UUID releasedBy;
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional computed fields
    private Long issueCount;
    private Long completedIssueCount;
    private Long uncompletedIssueCount;
    private Long percentComplete;
}