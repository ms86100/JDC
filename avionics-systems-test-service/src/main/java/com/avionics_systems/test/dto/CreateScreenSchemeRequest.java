package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateScreenSchemeRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Scheme name is required")
    @Size(max = 255, message = "Scheme name must not exceed 255 characters")
    private String name;

    private String description;

    private Boolean isDefault = false;
}