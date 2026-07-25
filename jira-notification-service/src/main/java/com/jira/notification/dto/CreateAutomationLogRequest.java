package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationLogRequest {

    @NotBlank(message = "{validation.automation.log.ruleId.required}")
    private UUID ruleId;

    @NotBlank(message = "{validation.automation.log.triggerType.required}")
    private String triggerType;

    private UUID triggerEventId;

    @NotBlank(message = "{validation.automation.log.status.required}")
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