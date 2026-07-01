package com.jira.test.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRunTrendResponse {
    private UUID testId;
    private int days;
    private List<TrendDataPoint> dataPoints;
    private TrendSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendDataPoint {
        private LocalDateTime timestamp;
        private String status;
        private Integer duration;
        private Double passRate;
        private int passedSteps;
        private int failedSteps;
        private int totalSteps;
        private String environment;
        private UUID runId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendSummary {
        private Double averagePassRate;
        private Double averageDuration;
        private Integer totalRuns;
        private Integer passedRuns;
        private Integer failedRuns;
        private Integer flakyRuns;
        private Double flakinessScore;
        private String trendDirection; // IMPROVING, DECLINING, STABLE, FLUCTUATING
        private Double trendSlope; // Positive = improving, negative = declining
        private DurationTrend durationTrend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DurationTrend {
        private Double averageDuration;
        private Double minDuration;
        private Double maxDuration;
        private String direction; // FASTER, SLOWER, STABLE
        private Double changePercent;
    }
}
