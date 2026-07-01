package com.jira.test.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaselineResponse {
    private UUID runId;
    private UUID testId;
    private UUID projectId;
    private String status;
    private LocalDateTime setAt;
    private UUID setBy;
    private String setByName;
    private int passedSteps;
    private int failedSteps;
    private int totalSteps;
    private Integer duration;
    private String environment;
}
