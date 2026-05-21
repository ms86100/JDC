package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String testType;
    private String status;
    private List<String> labels;
    private String priority;
    private UUID ownerId;
    private List<String> requirementKeys;
    private String gherkinFeatureKey;
    private String gherkinScenarioId;
    private UUID testSetId;
    private UUID folderId;
    private Boolean archived;
    private List<StepResponse> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepResponse {
        private UUID id;
        private Integer stepOrder;
        private String stepType;
        private String description;
        private String testData;
        private String expectedResult;
        private LocalDateTime createdAt;
    }
}