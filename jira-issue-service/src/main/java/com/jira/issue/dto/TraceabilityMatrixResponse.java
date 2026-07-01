package com.jira.issue.dto;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for traceability matrix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceabilityMatrixResponse {

    private UUID projectId;
    private Integer totalRequirements;
    private Integer totalTests;
    private Double overallCoverage;
    private List<RequirementRow> requirements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementRow {
        private String requirementKey;
        private String requirementSummary;
        private Integer testCount;
        private String coverageStatus;
        private List<TestCoverage> tests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCoverage {
        private UUID testId;
        private String testName;
        private String status;
        private String coverageStatus;
    }
}