package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkflowDefinitionRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private UUID projectId;

    private String workflowType;

    private String workflowStepsJson;

    private String transitionRulesJson;

    private Boolean isDefault;
}