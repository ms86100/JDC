package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Defect density report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectDensityResponse {

    private UUID projectId;
    private Integer totalDefects;
    private List<DefectDensityRow> requirements;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefectDensityRow {
        private String requirementKey;
        private Integer defectCount;
        private String severity;
        private String riskLevel; // HIGH, MEDIUM, LOW
    }
}