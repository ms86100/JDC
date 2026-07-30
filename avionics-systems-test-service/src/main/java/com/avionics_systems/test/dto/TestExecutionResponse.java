package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecutionResponse {
    private UUID id;
    private UUID testPlanId;
    private UUID testSetId;
    private UUID testId;
    private String name;
    private String description;
    private String status;
    private String testEnv;
    private UUID testerId;
    private String testCycle;
    private String ciBuildUrl;
    private String ciJobId;
    private Integer totalTests;
    private Integer passedTests;
    private Integer failedTests;
    private Integer blockedTests;
    private Integer notRunTests;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StepResultResponse> stepResults;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepResultResponse {
        private UUID id;
        private UUID stepId;
        private String status;
        private String actualResult;
        private List<String> evidenceUrls;
        private String defectKey;
        private String comment;
        private LocalDateTime executedAt;
    }
}