package com.jira.test.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRunResponse {
    private UUID id;
    private UUID testId;
    private UUID executionId;
    private UUID projectId;
    private String status;
    private UUID executedBy;
    private LocalDateTime executedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer duration;
    private String comment;
    private String defectKeys;
    private List<String> stepStatuses;
    private Integer passedSteps;
    private Integer failedSteps;
    private Integer blockedSteps;
    private Integer totalSteps;
    private String environment;
    private String browser;
    private String platform;
    private String testData;
    private List<String> evidenceLinks;
    private String logs;
    private String errorMessage;
    private Boolean isRetry;
    private UUID parentRunId;
    private String annotations;
    private List<String> tags;
    private Boolean isBaseline;
    private UUID baselineId;
    private Boolean isArchived;
    private LocalDateTime archivedAt;
    private Double flakinessScore;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}