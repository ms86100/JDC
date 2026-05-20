package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddedStepResponse {

    private UUID id;
    private UUID testId;
    private Integer stepIndex;
    private UUID sharedStepId;
    private String sharedStepName;
    private Integer sharedStepVersion;
    private List<SharedStepDto> embeddedSteps; // The expanded steps
    private String createdAt;
}