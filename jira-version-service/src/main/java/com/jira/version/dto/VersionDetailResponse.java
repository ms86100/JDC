package com.jira.version.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionDetailResponse {
    // Base fields from VersionResponse
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private Boolean released;
    private Boolean archived;
    private Integer sequence;
    private LocalDateTime startDate;
    private LocalDateTime releaseDate;
    private LocalDateTime actualReleaseDate;
    private String semanticVersion;
    private String buildNumber;
    private String branchName;
    private String releaseTrain;
    private String deploymentStatus;
    private String releaseStatus;
    private String releaseNotesUrl;
    private Boolean releaseNotesGenerated;
    private String color;
    private UUID createdBy;
    private UUID updatedBy;
    private UUID releasedBy;
    private UUID archivedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean overdue;
    private Long issueCount;
    private Long unresolvedIssueCount;
    private Long completedIssueCount;
    private Double progressPercentage;

    // Extended fields
    private List<VersionMetricsResponse> metricsHistory;
    private List<VersionDeploymentResponse> deployments;
    private List<VersionBuildReferenceResponse> builds;
    private VersionReleaseNoteResponse releaseNote;
}