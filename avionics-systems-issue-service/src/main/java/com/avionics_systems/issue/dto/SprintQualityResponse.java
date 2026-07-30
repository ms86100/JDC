package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sprint quality metrics report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintQualityResponse {

    private UUID projectId;
    private UUID sprintId;
    private Integer totalExecutions;
    private Integer totalTestsRun;
    private Integer testsPassed;
    private Integer testsFailed;
    private Integer testsBlocked;
    private Integer testsSkipped;
    private Double passRate;
    private Double passRateChange;
    private Double avgExecutionsPerDay;
    private Integer defectsFound;
    private String status; // EXCELLENT, GOOD, NEEDS_IMPROVEMENT, CRITICAL
    private LocalDateTime generatedAt;
}