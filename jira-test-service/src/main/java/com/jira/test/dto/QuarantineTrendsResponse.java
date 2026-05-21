package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineTrendsResponse {

    private List<TrendDataPoint> quarantineVolume;
    private List<TrendDataPoint> restorationVolume;
    private List<TrendDataPoint> netQuarantineChange;

    private List<TrendDataPoint> averageDurationTrend;
    private List<TrendDataPoint> resolutionRateTrend;

    private List<TriggerTypeTrend> triggerTypeDistribution;

    private List<FlakinessTrend> flakinessCorrelation;

    private TrendPeriod period;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int dataPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendDataPoint {
        private LocalDateTime date;
        private long count;
        private BigDecimal value;
        private String label;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TriggerTypeTrend {
        private String triggerType;
        private List<TrendDataPoint> trend;
        private long totalCount;
        private double percentageOfTotal;
        private double trendDirection; // positive = increasing, negative = decreasing
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlakinessTrend {
        private String flakyScoreBucket;
        private long quarantineCount;
        private long restorationCount;
        private double resolutionRate;
        private double avgTimeToResolution;
    }

    public enum TrendPeriod {
        DAILY,
        WEEKLY,
        MONTHLY,
        QUARTERLY
    }
}