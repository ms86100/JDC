package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Requirement coverage report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementCoverageResponse {

    private UUID projectId;
    private Integer totalRequirements;
    private Integer fullyCovered;
    private Integer partiallyCovered;
    private Integer failing;
    private Double overallCoverage;
    private List<RequirementCoverageRow> requirements;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementCoverageRow {
        private String requirementKey;
        private Integer testCount;
        private Integer testsPassed;
        private Integer testsFailed;
        private Double coveragePercent;
        private String status; // COVERED, PARTIAL, FAILING, NOT_COVERED
    }
}