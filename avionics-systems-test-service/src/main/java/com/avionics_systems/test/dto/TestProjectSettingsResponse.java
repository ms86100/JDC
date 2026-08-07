package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestProjectSettingsResponse {

    private UUID id;
    private UUID projectId;
    private String settings;
    private String defaultTestType;
    private String defaultPriority;
    private String defaultTestStatus;
    private Boolean autoCreateExecution;
    private Boolean requireApproval;
    private Integer retentionDays;
    private Integer maxStepsPerTest;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
