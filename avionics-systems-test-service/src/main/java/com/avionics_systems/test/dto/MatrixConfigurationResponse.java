package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatrixConfigurationResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private List<MatrixConfigurationRequest.DimensionConfig> dimensions;
    private List<MatrixConfigurationRequest.FilterRule> filterRules;
    private List<MatrixConfigurationRequest.ConflictRule> conflictRules;
    private Integer totalCombinations;
    private Integer validCombinations;
    private Integer invalidCombinations;
    private Boolean isActive;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}