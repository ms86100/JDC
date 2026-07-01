package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCoverageResponse {
    private UUID projectId;
    private BigDecimal overallCoverage;
    private int totalRequirements;
    private int totalTests;
    private int coveredTests;
    private int uncoveredTests;
    private Map<String, RequirementCoverage> byRequirement;
    private Map<String, TestSetCoverage> byTestSet;
    private LocalDateTime lastUpdated;
    private List<CoverageAlert> alerts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementCoverage {
        private String requirementKey;
        private UUID requirementId;
        private BigDecimal coverage;
        private int totalTests;
        private int coveredTests;
        private int uncoveredTests;
        private boolean meetsThreshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestSetCoverage {
        private UUID testSetId;
        private String testSetName;
        private BigDecimal coverage;
        private int totalTests;
        private int coveredTests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverageAlert {
        private String alertType;
        private String severity;
        private String message;
        private UUID relatedId;
        private String requirementKey;
    }
}
