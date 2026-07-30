package com.avionics_systems.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlakyTestResponse {

    private UUID testId;
    private String testIssueKey;
    private String testName;
    private Integer totalExecutions;
    private Integer totalFailures;
    private Integer totalPasses;
    private BigDecimal flakyScore;
    private String passRateTrend;
    private LocalDateTime firstFlakyOccurrence;
    private LocalDateTime lastFlakyOccurrence;
    private String currentStatus;
    private BigDecimal confidenceLevel;
    private List<FlakyPatternResponse> patterns;
    private List<ExecutionRecordResponse> recentExecutions;
}