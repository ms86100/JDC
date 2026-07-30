package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestPlanRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<String> labels;

    private UUID createdBy;
}