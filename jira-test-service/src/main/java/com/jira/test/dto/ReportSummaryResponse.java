package com.jira.test.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSummaryResponse {
    private long totalTests;
    private long totalTestSets;
    private long totalTestPlans;
    private long totalExecutions;
    private double overallPassRate;
    private int testsPassed;
    private int testsFailed;
    private int testsBlocked;
    private int testsNotRun;
    private LocalDateTime generatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestTrendPoint {
        private LocalDate date;
        private int passed;
        private int failed;
        private int blocked;
    }
}