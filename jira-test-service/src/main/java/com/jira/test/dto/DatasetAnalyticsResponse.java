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
public class DatasetAnalyticsResponse {

    private UUID datasetId;
    private String datasetName;
    private LocalDateTime generatedAt;

    // Usage patterns
    private UsageAnalytics usage;

    // Data coverage
    private CoverageAnalytics coverage;

    // Quality metrics
    private QualityAnalytics quality;

    // Freshness
    private FreshnessAnalytics freshness;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageAnalytics {
        private Integer totalExecutions; // Times used in test executions
        private Integer totalTestsBound; // Number of tests using this dataset
        private Integer uniqueProjects; // Number of projects using this dataset

        private LocalDateTime lastUsed;
        private LocalDateTime firstUsed;
        private Integer usageTrendDays; // Days of usage history

        private List<UsageByDate> usageOverTime;
        private Map<String, Integer> usageByTestType;
        private List<String> topBoundTests;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageByDate {
        private LocalDateTime date;
        private Integer count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoverageAnalytics {
        private Integer totalColumns;
        private Integer totalRows;
        private Double columnCoveragePercent; // Non-null columns
        private Double dataCoveragePercent; // Non-null cells

        private Map<String, ColumnCoverage> columnLevelCoverage;

        private Double densityScore; // 0-100, how dense is the data
        private Integer emptyRows;
        private Integer duplicateRows;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnCoverage {
        private String columnName;
        private Integer nullCount;
        private Integer uniqueCount;
        private Double completenessPercent;
        private String recommendedType;
        private List<String> sampleValues;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QualityAnalytics {
        private Double overallQualityScore; // 0-100

        private Double consistencyScore;
        private Double validityScore;
        private Double uniquenessScore;
        private Double completenessScore;

        private List<QualityIssue> issues;
        private List<String> recommendations;

        private Map<String, Double> typeDistribution; // Value types found
        private Map<String, Integer> valueFrequency; // Most common values
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QualityIssue {
        private String issueType; // INCONSISTENT, INVALID, DUPLICATE, INCOMPLETE
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String description;
        private Integer affectedRows;
        private String affectedColumn;
        private String suggestedFix;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FreshnessAnalytics {
        private LocalDateTime lastModified;
        private Integer currentVersion;
        private Integer totalVersions;

        private Integer daysSinceLastUpdate;
        private String updateFrequency; // DAILY, WEEKLY, MONTHLY, RARELY, NEVER

        private Double stalenessScore; // 0-100, 0 = very fresh, 100 = stale
        private List<VersionAge> versionHistory;

        private Boolean hasNewerVersion;
        private Integer versionsBehindLatest;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VersionAge {
        private Integer versionNumber;
        private LocalDateTime createdAt;
        private Integer ageInDays;
    }
}