package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineStatsResponse {

    private long totalQuarantined;
    private long activeQuarantined;
    private long underReview;
    private long restored;

    private BigDecimal averageQuarantineDurationDays;
    private BigDecimal medianQuarantineDurationDays;

    private BigDecimal resolutionRate; // percentage restored vs total

    private Map<String, Long> byStatus;
    private Map<String, Long> byTriggerType;
    private Map<String, Long> byProject;

    private long quarantinedThisMonth;
    private long quarantinedLastMonth;
    private BigDecimal monthOverMonthChange;

    private long restoredThisMonth;
    private long restoredLastMonth;
    private BigDecimal restorationRateChange;

    private LocalDateTime lastCalculated;

    // Flakiness metrics
    private double autoQuarantineRate;
    private double manualQuarantineRate;

    // Coverage impact
    private int testsQuarantinedToday;
    private int testsQuarantinedThisWeek;
    private int testsQuarantinedThisMonth;

    // Cost savings
    private BigDecimal estimatedCostSavingsHours;
    private BigDecimal estimatedCostSavingsCurrency;

    private List<QuarantineRuleStats> topRulesByTriggers;
    private List<ProjectQuarantineSummary> projectSummaries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuarantineRuleStats {
        private UUID ruleId;
        private String ruleName;
        private String ruleType;
        private long triggerCount;
        private long activeCount;
        private double effectivenessScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectQuarantineSummary {
        private UUID projectId;
        private String projectName;
        private long totalQuarantined;
        private long currentlyActive;
        private double restorationRate;
        private BigDecimal avgDurationDays;
    }
}