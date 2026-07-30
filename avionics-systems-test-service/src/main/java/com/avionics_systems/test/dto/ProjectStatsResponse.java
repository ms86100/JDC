package com.avionics_systems.test.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStatsResponse {
    private UUID projectId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;

    // Overall counts
    private long totalRuns;
    private long passedRuns;
    private long failedRuns;
    private long blockedRuns;
    private long pendingRuns;

    // Pass rate
    private Double overallPassRate;
    private Double passRateChange; // Change from previous period

    // Duration stats
    private Double averageDuration;
    private Double medianDuration;
    private Double minDuration;
    private Double maxDuration;
    private DurationTrend durationTrend;

    // Flakiness
    private Double overallFlakinessScore;
    private List<FlakyTestInfo> flakyTests;

    // Environment breakdown
    private Map<String, EnvironmentStats> environmentStats;

    // Trends over time
    private List<DailyTrend> dailyTrends;

    // Tag distribution
    private Map<String, Integer> tagDistribution;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DurationTrend {
        private Double previousAverage;
        private Double currentAverage;
        private Double changePercent;
        private String direction; // FASTER, SLOWER, STABLE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlakyTestInfo {
        private UUID testId;
        private String testName;
        private Double flakinessScore;
        private int totalRuns;
        private int passCount;
        private int failCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnvironmentStats {
        private String environment;
        private long totalRuns;
        private long passedRuns;
        private long failedRuns;
        private Double passRate;
        private Double averageDuration;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyTrend {
        private LocalDateTime date;
        private long totalRuns;
        private long passedRuns;
        private long failedRuns;
        private Double passRate;
        private Double averageDuration;
    }
}
