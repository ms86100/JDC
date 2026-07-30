package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetentionPolicyResponse {

    private UUID id;
    private UUID projectId;
    private String policyName;
    private String description;
    private String evidenceType;
    private Integer retentionDays;
    private Boolean compressionEnabled;
    private Boolean autoArchive;
    private Boolean moveToColdStorage;
    private Integer coldStorageAfterDays;
    private Boolean permanentDelete;
    private Integer deleteAfterDays;
    private Boolean isActive;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}