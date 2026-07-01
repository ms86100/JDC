package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceabilityMatrixResponse {
    private List<RequirementRow> requirements;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequirementRow {
        private String requirementKey;
        private String requirementType;
        private long testCount;
        private List<TestCoverage> tests;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCoverage {
        private UUID testId;
        private String testName;
        private String status;
        private Integer executionPassRate;
        private String lastExecutionStatus;
    }
}