package com.avionics_systems.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationLogResponse {

    private UUID id;
    private UUID ruleId;
    private String triggerType;
    private UUID triggerEventId;
    private String status;
    private String message;
    private Integer conditionsEvaluated;
    private Integer conditionsPassed;
    private Integer actionsExecuted;
    private Integer actionsFailed;
    private Long executionTimeMs;
    private String errorDetails;
    private String contextData;
    private OffsetDateTime createdAt;
}