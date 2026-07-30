package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionRecordResponse {

    private UUID id;
    private UUID executionId;
    private UUID testId;
    private Boolean isFlakyExecution;
    private String failureReason;
    private UUID environmentId;
    private Integer executionDurationMs;
    private Integer retryAttempt;
    private LocalDateTime analyzedAt;
}