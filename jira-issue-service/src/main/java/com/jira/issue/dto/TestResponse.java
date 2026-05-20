package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResponse {

    private UUID id;
    private UUID projectId;
    private String issueKey;
    private String name;
    private String description;
    private String testType;
    private String testStatus;
    private String priority;
    private UUID ownerId;
    private List<String> labels;
    private List<String> requirementKeys;
    private String gherkinFeatureKey;
    private String gherkinScenarioId;
    private UUID testSetId;
    private UUID folderId;
    private String lastExecutionStatus;
    private List<StepResponse> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResponse {
        private Integer stepOrder;
        private String stepType;
        private String description;
        private String testData;
        private String expectedResult;
    }
}