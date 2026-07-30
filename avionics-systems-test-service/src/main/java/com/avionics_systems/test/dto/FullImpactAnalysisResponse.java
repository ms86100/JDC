package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FullImpactAnalysisResponse {

    private UUID sharedStepId;
    private String sharedStepName;
    private Integer currentVersion;

    // Impact summary
    private Integer totalAffectedTests;
    private Integer totalAffectedTestPlans;
    private Integer totalAffectedTestSets;

    // Impact by test status
    private Map<String, Integer> testsByStatus;

    // Impact by priority/severity
    private Map<String, Integer> testsByPriority;

    // Impact distribution
    private ImpactDistribution distribution;

    // Risk assessment
    private RiskAssessment riskAssessment;

    // Detailed impact list
    private List<ImpactDetail> affectedTests;

    // Cascading impact (shared steps used by affected tests)
    private List<CascadingImpact> cascadingImpacts;

    // Recommendations
    private List<String> recommendations;

    // Execution history
    private ExecutionImpact executionImpact;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImpactDistribution {
        private Integer lowImpact; // Tests with 1 mapping
        private Integer mediumImpact; // Tests with 2-5 mappings
        private Integer highImpact; // Tests with 6+ mappings
        private Double impactScore; // 0.0 to 1.0
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskAssessment {
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private Double riskScore;
        private List<String> riskFactors;
        private List<String> mitigationSteps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImpactDetail {
        private UUID testId;
        private String testName;
        private String testStatus;
        private Integer priority;
        private Integer mappingCount;
        private String lastExecuted;
        private String lastExecutionStatus;
        private LocalDateTime createdAt;
        private List<UUID> requirementIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CascadingImpact {
        private UUID sharedStepId;
        private String sharedStepName;
        private Integer affectedTestCount;
        private String relationshipType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutionImpact {
        private Integer totalExecutionsLast30Days;
        private Double averagePassRate;
        private Integer testsNeedingRerun;
        private List<String> affectedEnvironments;
    }
}