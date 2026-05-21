package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepVersionDiffResponse {

    private UUID sharedStepId;
    private String sharedStepName;

    private Integer fromVersion;
    private Integer toVersion;

    // Summary
    private Integer stepsAdded;
    private Integer stepsRemoved;
    private Integer stepsModified;

    // Detailed Changes
    private List<StepChange> stepChanges;

    // Version Details
    private SharedStepVersionResponse fromVersionDetails;
    private SharedStepVersionResponse toVersionDetails;

    // Change Metadata
    private String changeMagnitude; // MINOR, MODERATE, MAJOR
    private Boolean breakingChange; // Changes that may affect test behavior

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepChange {
        private Integer stepOrder;
        private String changeType; // ADDED, REMOVED, MODIFIED
        private String stepType;
        private String beforeDescription;
        private String afterDescription;
        private String beforeExpectedResult;
        private String afterExpectedResult;
        private List<String> changedFields; // Which fields changed
        private Double similarityScore; // 0.0 to 1.0
    }
}