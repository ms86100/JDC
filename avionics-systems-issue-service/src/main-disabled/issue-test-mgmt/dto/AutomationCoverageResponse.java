package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Automation coverage report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationCoverageResponse {

    private UUID projectId;
    private Integer totalTests;
    private Integer automatedTests;
    private Integer manualTests;
    private Integer bddTests;
    private Double automationPercent;
    private Integer recentlyAutomated;
    private Integer requirementsWithoutAutomation;
    private List<String> uncoveredRequirementKeys;
    private LocalDateTime generatedAt;
}