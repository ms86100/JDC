package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for step result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResultResponse {

    private UUID id;
    private UUID executionId;
    private UUID issueId;
    private Integer stepOrder;
    private String stepType;
    private String stepDescription;
    private String expectedResult;
    private String status;
    private String actualResult;
    private String defectKey;
    private String defectSeverity;
    private LocalDateTime executedAt;
    private Long executionTimeMs;
    private String comment;
    private LocalDateTime createdAt;
}