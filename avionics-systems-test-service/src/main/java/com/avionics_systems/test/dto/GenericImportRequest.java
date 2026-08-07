package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericImportRequest {
    @NotNull
    private UUID projectId;
    private UUID testSetId;
    private List<GenericTestResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenericTestResult {
        private String name;
        private String status;
        private Double duration;
        private List<GenericTestStep> steps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenericTestStep {
        private String description;
        private String expectedResult;
        private String actualResult;
        private String status;
    }
}
