package com.avionics_systems.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloneProjectRequest {

    @NotBlank(message = "{validation.project.name.required}")
    private String name;

    @NotBlank(message = "{validation.project.key.required}")
    @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$", message = "{validation.project.key.format}")
    private String projectKey;
}