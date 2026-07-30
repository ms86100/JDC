package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementImpactDto {
    private String requirementKey;
    private String requirementTitle;
    private Integer fromVersion;
    private Integer toVersion;
    private String changeType; // ADDED, MODIFIED, REMOVED
    private String changeSummary;
    private Integer affectedTestsCount;
    private List<TestImpactDetailDto> affectedTests;
    private String riskLevel;
    private List<String> suggestedActions;
}