package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BindDatasetRequest {

    @NotNull(message = "Test ID is required")
    private UUID testId;

    @NotNull(message = "Dataset ID is required")
    private UUID datasetId;

    private UUID datasetVersionId;

    private Map<String, String> columnMappings; // e.g., {"username": "${username}", "password": "${password}"}
}