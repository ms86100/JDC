package com.avionics_systems.test.dto;

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
public class FlakyDashboardResponse {

    private Integer totalTestsAnalyzed;
    private Integer stableCount;
    private Integer flakyCount;
    private Integer quarantineCandidateCount;
    private BigDecimal averageFlakyScore;
    private List<FlakyTestResponse> topFlakyTests;
    private Map<String, Integer> patternsByType;
    private List<FlakyTrendResponse> trends;
}