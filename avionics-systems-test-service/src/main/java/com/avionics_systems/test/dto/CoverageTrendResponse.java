package com.avionics_systems.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageTrendResponse {
    private UUID projectId;
    private int periodDays;
    private List<TrendDataPoint> trendPoints;
    private TrendSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private LocalDateTime date;
        private BigDecimal overallCoverage;
        private BigDecimal requirementCoverage;
        private BigDecimal testSetCoverage;
        private int totalTests;
        private int executedTests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendSummary {
        private BigDecimal currentCoverage;
        private BigDecimal sevenDayAverage;
        private BigDecimal thirtyDayAverage;
        private BigDecimal ninetyDayAverage;
        private BigDecimal changeRate7d;
        private BigDecimal changeRate30d;
        private BigDecimal changeRate90d;
        private String trendDirection;
    }
}