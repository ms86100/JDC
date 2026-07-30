package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthTrendResponse {

    private UUID projectId;

    private List<TrendDataPoint> folderCountTrend;
    private List<TrendDataPoint> testCountTrend;
    private List<TrendDataPoint> executionTrend;

    private Integer foldersAddedLast30Days;
    private Integer foldersAddedLast90Days;
    private Integer testsAddedLast30Days;
    private Integer testsAddedLast90Days;

    private Double folderGrowthRate;
    private Double testGrowthRate;

    private String growthClassification;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class TrendDataPoint {
    private LocalDate date;
    private Integer value;
    private String label;
}