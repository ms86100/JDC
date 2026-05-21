package com.jira.test.dto;

import com.jira.test.entity.CoverageDriftRecord;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftAnalysisResponse {
    private UUID id;
    private UUID requirementId;
    private UUID projectId;
    private BigDecimal previousCoverage;
    private BigDecimal currentCoverage;
    private BigDecimal drift;
    private CoverageDriftRecord.DriftType driftType;
    private Integer previousTestCount;
    private Integer currentTestCount;
    private List<AffectedTestInfo> affectedTests;
    private List<String> missingCoverage;
    private List<String> staleCoverage;
    private Boolean actionRequired;
    private LocalDateTime detectedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AffectedTestInfo {
        private UUID testId;
        private String testKey;
        private String linkType;
    }
}