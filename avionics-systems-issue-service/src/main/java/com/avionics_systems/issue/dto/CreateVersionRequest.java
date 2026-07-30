package com.avionics_systems.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVersionRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Version name is required")
    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate releaseDate;

    private Integer sortOrder;

    private Boolean isReleased;

    private Boolean isArchived;
}