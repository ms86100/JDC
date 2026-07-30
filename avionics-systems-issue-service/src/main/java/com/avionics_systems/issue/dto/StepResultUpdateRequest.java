package com.avionics_systems.issue.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating step result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResultUpdateRequest {

    private String stepType;
    private String stepDescription;
    private String expectedResult;
    private String status; // PASSED, FAILED, BLOCKED, SKIPPED, NOT_RUN
    private String actualResult;
    private Long executionTimeMs;
    private String defectKey;
    private String defectSeverity;
    private String comment;
    private List<UUID> evidenceIds;
}