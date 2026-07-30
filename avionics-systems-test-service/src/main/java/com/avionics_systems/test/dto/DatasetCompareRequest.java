package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetCompareRequest {

    @NotNull(message = "First dataset ID is required")
    private UUID datasetId1;

    @NotNull(message = "Second dataset ID is required")
    private UUID datasetId2;

    private UUID projectId;

    // Comparison options
    private Boolean compareStructure = true; // Compare column names and types
    private Boolean compareData = true; // Compare actual data values
    private Boolean caseSensitive = true;
    private Boolean ignoreWhitespace = false;
    private Double numericTolerance = 0.0; // Tolerance for floating point comparison

    // Specific columns to compare (null = compare all)
    private java.util.List<String> columnsToCompare;
}