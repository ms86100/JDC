package com.jira.test.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRunComparisonResponse {
    private TestRunResponse run1;
    private TestRunResponse run2;

    // Summary differences
    private ComparisonSummary summary;

    // Step-by-step comparison
    private List<StepComparison> stepComparisons;

    // Duration comparison
    private DurationComparison durationComparison;

    // Status comparison
    private StatusComparison statusComparison;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComparisonSummary {
        private int totalSteps1;
        private int totalSteps2;
        private int passedSteps1;
        private int passedSteps2;
        private int failedSteps1;
        private int failedSteps2;
        private int passedDiff;
        private int failedDiff;
        private int newPassed;
        private int newFailed;
        private int fixedTests;
        private Double passRateChange;
        private Integer durationChange;
        private String verdict; // IMPROVED, REGRESSED, STABLE, NEW_FAILURES, FIXED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepComparison {
        private int stepIndex;
        private String status1;
        private String status2;
        private boolean changed;
        private String changeType; // FIXED, BROKE, SAME
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DurationComparison {
        private Integer duration1;
        private Integer duration2;
        private Integer changeSeconds;
        private Double changePercent;
        private String verdict; // FASTER, SLOWER, SAME
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusComparison {
        private String status1;
        private String status2;
        private boolean improved;
        private boolean regressed;
        private String verdict; // IMPROVED, REGRESSED, SAME
    }
}
