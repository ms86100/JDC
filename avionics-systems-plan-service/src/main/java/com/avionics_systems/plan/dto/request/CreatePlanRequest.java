package com.avionics_systems.plan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequest {

    @NotBlank(message = "Plan name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    private UUID ownerId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Map<String, Object> settings;

    private UUID programId;
}