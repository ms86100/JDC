package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationLogRequest {

    @NotBlank(message = "Rule ID is required")
    private UUID ruleId;

    @NotBlank(message = "Trigger type is required")
    private String triggerType;

    private UUID triggerEventId;

    @NotBlank(message = "Status is required")
    private String status;

    private String message;
    private Integer conditionsEvaluated;
    private Integer conditionsPassed;
    private Integer actionsExecuted;
    private Integer actionsFailed;
    private Long executionTimeMs;
    private String errorDetails;
    private String contextData;
}