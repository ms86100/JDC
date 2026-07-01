package com.jira.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Test Summary Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSummaryResponse {

    private UUID projectId;

    // Test counts
    private int totalTests;
    private int manualTests;
    private int automatedTests;
    private int bddTests;

    // Test status counts
    private int draftTests;
    private int readyTests;
    private int approvedTests;
    private int deprecatedTests;

    // Execution summary
    private int totalExecutions;
    private int passedExecutions;
    private int failedExecutions;
    private int blockedExecutions;

    // Coverage
    private double requirementCoverage;
    private int coveredRequirements;
    private int totalRequirements;
}
