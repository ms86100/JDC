package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestImpactDetailDto {
    private String testId;
    private String testIssueKey;
    private String testName;
    private String testType;
    private String status;
    private String impactLevel;
    private Double riskScore;
    private String reason;
    private Integer cascadeLevel;
    private String componentName;
    private String requirementKey;
    private List<String> dependentTests;
    private List<String> mitigationSuggestions;
}