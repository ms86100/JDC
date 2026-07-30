package com.avionics_systems.report.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintReportResponse {

    private UUID id;
    private UUID sprintId;
    private String sprintName;
    private UUID projectId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalIssues;
    private Integer completedIssues;
    private Integer incompleteIssues;
    private Integer bugsCount;
    private Double completionRate;
    private Double totalStoryPoints;
    private Double completedStoryPoints;
    private Long totalTimeSeconds;
    private String issuesCompleted;
    private String issuesAddedDuringSprint;
    private String issuesNotCompleted;
    private String issuesLedged;
    private String burndownData;
    private LocalDateTime createdAt;
}