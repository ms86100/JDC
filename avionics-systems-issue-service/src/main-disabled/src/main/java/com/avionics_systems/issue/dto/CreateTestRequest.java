package com.avionics_systems.issue.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestRequest {

    private String name;
    private String description;
    private String testType; // MANUAL, AUTOMATED, BDD
    private String testStatus; // DRAFT, READY, APPROVED, DEPRECATED
    private String priority; // CRITICAL, HIGH, MEDIUM, LOW
    private UUID ownerId;
    private List<String> labels;
    private List<String> requirementKeys;
    private List<TestStepDto> steps;
    private UUID testSetId;
    private UUID folderId;
    private String gherkinFeatureKey;
    private String gherkinScenarioId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStepDto {
        private Integer stepOrder;
        private String stepType; // GIVEN, WHEN, THEN, AND, BUT
        private String description;
        private String testData;
        private String expectedResult;
    }
}