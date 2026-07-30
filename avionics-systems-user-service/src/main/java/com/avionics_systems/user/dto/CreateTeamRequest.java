package com.avionics_systems.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeamRequest {

    @NotNull(message = "{validation.organization.id.required}")
    private UUID organizationId;

    @NotBlank(message = "{validation.team.name.required}")
    @Size(max = 255, message = "{validation.team.name.size}")
    private String name;

    @Size(max = 2000, message = "{validation.team.description.size}")
    private String description;
}