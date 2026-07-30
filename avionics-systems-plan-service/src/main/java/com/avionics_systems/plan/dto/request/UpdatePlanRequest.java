package com.avionics_systems.plan.dto.request;

import lombok.*;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlanRequest {

    private String name;
    private String description;
    private UUID ownerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> settings;
    private Boolean isActive;

    /**
     * Version for optimistic locking.
     * If provided, the update will fail with 409 Conflict if the entity
     * has been modified by another user since the version was read.
     */
    @Min(0)
    private Long version;
}