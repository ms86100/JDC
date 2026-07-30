package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetentionPolicyRequest {

    private UUID projectId;

    @NotNull(message = "Policy name is required")
    private String policyName;

    private String description;

    private String evidenceType; // SCREENSHOT, VIDEO, LOG, etc.

    private Integer retentionDays;

    private Boolean compressionEnabled;

    private Boolean autoArchive;

    private Boolean moveToColdStorage;

    private Integer coldStorageAfterDays;

    private Boolean permanentDelete;

    private Integer deleteAfterDays;

    private UUID createdBy;
}