package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsertSharedStepRequest {

    @NotNull(message = "Test ID is required")
    private UUID testId;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Shared Step ID is required")
    private UUID sharedStepId;

    @NotNull(message = "Position is required")
    private Integer position; // Position to insert at

    private Map<String, String> parameters; // Override parameters

    private UUID sharedStepVersionId; // Specific version, or null for latest
}