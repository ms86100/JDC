package com.jira.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDashboardResponse {
    private UUID projectId;
    private VvoMetrics vvoMetrics;
    private DefectMetrics techEventMetrics;
    private DefectMetrics benchDefectMetrics;
    private DefectMetrics problemReportMetrics;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VvoMetrics {
        private int total;
        private int newCount;
        private int verifiedCount;
        private int releasedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefectMetrics {
        private int total;
        private int openCount;
        private int blockingCount;
    }
}
