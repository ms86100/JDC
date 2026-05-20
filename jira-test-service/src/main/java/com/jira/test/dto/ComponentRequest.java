package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Component name is required")
    private String componentName;

    private String componentPath;

    private String ownershipTeam;

    private String ownershipContact;

    private Map<String, Object> metadata;
}