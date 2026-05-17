package com.jira.version.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionMetricsResponse {
    private UUID versionId;
    private String versionName;
    private LocalDate snapshotDate;
    private Integer totalIssues;
    private Integer openIssues;
    private Integer closedIssues;
    private Integer resolvedIssues;
    private Double progressPercentage;
    private Double totalStoryPoints;
    private Double completedStoryPoints;
    private Double velocityPoints;
}