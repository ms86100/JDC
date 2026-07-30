package com.avionics_systems.plan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReleaseRequest {

    @NotBlank(message = "Release name is required")
    private String name;

    private String version;

    private String description;

    private LocalDate releaseDate;
}