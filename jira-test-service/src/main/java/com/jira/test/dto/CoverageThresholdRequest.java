package com.jira.test.dto;

import com.jira.test.entity.CoverageThreshold;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageThresholdRequest {
    private UUID projectId;
    private UUID requirementId;
    private String requirementKey;
    private BigDecimal minimumCoverage;
    private BigDecimal warningThreshold;
    private Boolean alertEnabled;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageThresholdResponse {
    private UUID id;
    private UUID projectId;
    private UUID requirementId;
    private String requirementKey;
    private BigDecimal minimumCoverage;
    private BigDecimal warningThreshold;
    private BigDecimal currentCoverage;
    private LocalDateTime lastChecked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CoverageThreshold.AlertLevel alertLevel;

    public static CoverageThresholdResponse from(CoverageThreshold threshold) {
        return CoverageThresholdResponse.builder()
                .id(threshold.getId())
                .projectId(threshold.getProjectId())
                .requirementId(threshold.getRequirementId())
                .requirementKey(threshold.getRequirementKey())
                .minimumCoverage(threshold.getMinimumCoverage())
                .warningThreshold(threshold.getWarningThreshold())
                .currentCoverage(threshold.getCurrentCoverage())
                .lastChecked(threshold.getLastChecked())
                .createdAt(threshold.getCreatedAt())
                .updatedAt(threshold.getUpdatedAt())
                .alertLevel(threshold.getAlertLevel())
                .build();
    }
}