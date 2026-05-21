package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepAnalyticsResponse {

    private UUID sharedStepId;
    private String sharedStepName;

    // Usage Statistics
    private Integer totalUsageCount;
    private Integer activeTestsCount;
    private Integer archivedTestsCount;

    // Popularity Metrics
    private Integer popularityRank;
    private Double usageScore;

    // Change Frequency
    private Integer totalVersions;
    private Integer versionChangeCount30Days;
    private Integer versionChangeCount90Days;
    private LocalDateTime lastModified;
    private Double modificationFrequency; // versions per month

    // Usage Over Time
    private List<UsageTrendPoint> usageTrend;

    // Maintenance Recommendations
    private List<String> recommendations;
    private String healthStatus; // HEALTHY, NEEDS_ATTENTION, HIGH_RISK

    // Test Status Distribution
    private Map<String, Long> testStatusDistribution;

    // Recent Usage
    private List<RecentUsageDetail> recentUsage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageTrendPoint {
        private String date;
        private Integer usageCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentUsageDetail {
        private UUID testId;
        private String testName;
        private LocalDateTime usedAt;
    }
}