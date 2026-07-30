package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private List<SharedStepDto> steps;
    private Integer currentVersion;
    private Integer usageCount;
    private UUID folderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional computed fields
    private List<SharedStepVersionResponse> versions;
    private List<SharedStepImpactResponse> impact;
}