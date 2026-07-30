package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Cucumber import
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CucumberImportResponse {

    private UUID batchId;
    private String featureKey;
    private Integer scenariosImported;
    private Integer testsCreated;
    private Integer testsUpdated;
    private String status;
    private String errorMessage;
}