package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary report for test execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {

    private UUID projectId;
    private UUID sprintId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Test counts
    private Integer totalTests;
    private Integer testsDraft;
    private Integer testsReady;
    private Integer testsApproved;
    private Integer testsDeprecated;
    private Integer testsManual;
    private Integer testsAutomated;
    private Integer testsBdd;

    // Execution counts
    private Integer totalExecutions;
    private Integer executionsPassed;
    private Integer executionsFailed;
    private Integer executionsBlocked;
    private Integer executionsRunning;

    // Test results
    private Integer totalTestsRun;
    private Integer testsPassed;
    private Integer testsFailed;
    private Double passRate;

    // Test sets
    private Integer totalTestSets;

    // Coverage
    private Integer totalRequirements;
    private Integer coveredRequirements;
    private Double requirementCoverage;

    // Defects
    private Integer totalDefects;
    private Integer criticalDefects;
    private Integer majorDefects;
    private Integer openDefects;

    private LocalDateTime generatedAt;
}