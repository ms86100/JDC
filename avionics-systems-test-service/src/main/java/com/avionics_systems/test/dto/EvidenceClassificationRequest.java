package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceClassificationRequest {

    @NotNull(message = "Evidence ID is required")
    private UUID evidenceId;

    @NotNull(message = "Classification level is required")
    private String classificationLevel; // STEP_LEVEL, RUN_LEVEL, ENVIRONMENT_LEVEL

    private String classificationReason;
}