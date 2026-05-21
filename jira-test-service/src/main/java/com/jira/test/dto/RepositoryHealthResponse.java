package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryHealthResponse {

    private UUID projectId;

    private Double overallHealthScore;

    private Integer totalFolders;
    private Integer totalTests;
    private Integer orphanedTests;
    private Integer emptyFolders;
    private Integer deepFolders;

    private Double testCoveragePercent;
    private Double executionRate;

    private GrowthTrendResponse growthTrend;

    private List<MaintenanceRecommendation> recommendations;

    private Map<String, Integer> healthByFolder;
    private Map<String, Double> passRateByFolder;
}