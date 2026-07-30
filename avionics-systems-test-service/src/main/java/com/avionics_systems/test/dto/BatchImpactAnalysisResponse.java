package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchImpactAnalysisResponse {
    private Integer totalAnalyzed;
    private Integer totalAffected;
    private Double overallRiskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<TestImpactDetailDto> allAffectedTests;
    private List<ImpactGraphDto> graphData;
    private List<String> suggestedSuites;
    private List<String> mitigationSummary;
}