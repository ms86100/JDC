package com.avionics_systems.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationSchemeRequest {

    @NotBlank(message = "{validation.scheme.name.required}")
    private String name;

    private String description;

    private UUID projectId;

    private Boolean isDefault;
}