package com.jira.report.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReportResponse {

    private UUID id;
    private String name;
    private UUID projectId;
    private String projectKey;
    private LocalDateTime reportDate;
    private Integer totalIssues;
    private Integer openIssues;
    private Integer resolvedIssues;
    private Double totalStoryPoints;
    private Double completedStoryPoints;
    private Double velocity;
    private String issuesByType;
    private String issuesByStatus;
    private String issuesByPriority;
    private String recentActivity;
    private String reportType;
    private LocalDateTime createdAt;
}