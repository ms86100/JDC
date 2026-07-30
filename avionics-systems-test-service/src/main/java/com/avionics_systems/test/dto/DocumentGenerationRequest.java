package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGenerationRequest {

    @NotNull
    private UUID templateId;

    private UUID projectId;

    private UUID fixVersionId;

    private UUID testPlanId;
}
