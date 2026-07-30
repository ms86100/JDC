package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineResponse {

    private UUID id;
    private UUID testId;
    private String testIssueKey;
    private String testName;
    private String status;
    private String quarantineReason;
    private String triggerType;
    private UUID triggeredBy;
    private LocalDateTime triggeredAt;
    private Boolean autoRestoreEnabled;
    private Map<String, Object> autoRestoreConditions;
    private Integer currentExecutionCount;
    private Integer currentPassCount;
    private LocalDateTime lastExecutionAt;
    private String lastStatus;
    private LocalDateTime restoredAt;
    private UUID restoredBy;
    private String restoreReason;
    private LocalDateTime createdAt;
}