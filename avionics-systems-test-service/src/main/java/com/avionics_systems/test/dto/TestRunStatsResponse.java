package com.avionics_systems.test.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRunStatsResponse {
    private UUID testId;
    private long totalRuns;
    private long passedRuns;
    private long failedRuns;
    private long blockedRuns;
    private long pendingRuns;
    private Double passRate;
    private Double averageDuration; // in seconds
    private Double flakyScore; // 0.0 to 1.0, higher = more flaky
    private LocalDateTime lastRunAt;
    private String lastRunStatus;
    private boolean isFlaky;
}