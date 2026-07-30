package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptExecutionLogResponse {

    private UUID id;
    private UUID scriptId;
    private String scriptKey;
    private String scriptType;
    private String executionMode;
    private UUID issueId;
    private UUID projectId;
    private UUID userId;
    private boolean success;
    private String resultValue;
    private String errorMessage;
    private long executionMs;
    private LocalDateTime createdAt;
}
