package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintReportResponse {
    private UUID sprintId;
    private String sprintName;
    private String sprintGoal;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime completeDate;
    private String state;

    private List<SprintIssueResponse> completedIssues;
    private List<SprintIssueResponse> issuesNotCompletedInCurrentSprint;
    private List<SprintIssueResponse> puntedIssues;
    private List<String> issueKeysAddedDuringSprint;

    private int committedPoints;
    private int completedPoints;
    private int scopeChangePoints;
    private int totalIssues;
    private int completedIssueCount;
    private int inProgressIssueCount;
    private int todoIssueCount;
    private double completionRate;
}
