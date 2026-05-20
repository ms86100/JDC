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
public class CreateExecutionRequest {

    private UUID testPlanId;
    private UUID testSetId;
    private UUID testId;

    @NotBlank
    private String name;

    private String description;

    private String testEnv;

    private UUID testerId;

    private String testCycle;

    private String ciBuildUrl;

    private String ciJobId;

    private List<StepResultDto> stepResults;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepResultDto {
        private UUID stepId;
        private String status;
        private String actualResult;
        private List<String> evidenceUrls;
        private String defectKey;
        private String comment;
    }
}