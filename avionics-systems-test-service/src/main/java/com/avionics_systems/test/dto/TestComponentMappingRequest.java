package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestComponentMappingRequest {

    @NotNull(message = "Test ID is required")
    private UUID testId;

    @NotNull(message = "Component ID is required")
    private UUID componentId;

    private BigDecimal confidenceScore;

    private String mappingType; // direct, indirect, ai-suggested
}