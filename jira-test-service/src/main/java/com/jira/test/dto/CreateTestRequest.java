package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String name;

    private String description;

    private String testType;

    private List<String> labels;

    private String priority;

    private UUID ownerId;

    private List<String> requirementKeys;

    private UUID folderId;

    private List<TestStepDto> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestStepDto {
        private Integer stepOrder;
        private String stepType;
        private String description;
        private String testData;
        private String expectedResult;
    }
}