package com.avionics_systems.sprint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSprintRequest {

    @NotBlank(message = "Sprint name is required")
    private String name;

    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Project ID is required")
    private UUID projectId;
}
