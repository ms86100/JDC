package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestProjectSettingsRequest {

    @NotNull
    private UUID projectId;

    private String settings;
    private String defaultTestType;
    private String defaultPriority;
    private String defaultTestStatus;
    private Boolean autoCreateExecution;
    private Boolean requireApproval;
    private Integer retentionDays;
    private Integer maxStepsPerTest;
}
