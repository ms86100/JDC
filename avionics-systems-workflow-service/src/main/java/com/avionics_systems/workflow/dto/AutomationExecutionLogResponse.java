package com.avionics_systems.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationExecutionLogResponse {

    private UUID id;
    private UUID ruleId;
    private UUID triggerIssueId;
    private String triggerEvent;
    private String status;
    private Integer actionsExecuted;
    private String errorMessage;
    private Integer executionDurationMs;
    private LocalDateTime executedAt;
}
