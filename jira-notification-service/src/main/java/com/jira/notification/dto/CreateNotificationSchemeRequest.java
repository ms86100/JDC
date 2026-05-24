package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationSchemeRequest {

    @NotBlank(message = "Scheme name is required")
    private String name;

    private String description;

    private UUID projectId;

    private Boolean isDefault;
}