package com.avionics_systems.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {

    private UUID projectId;

    @NotBlank(message = "{validation.workflow.name.required}")
    private String name;

    private String description;

    @Builder.Default
    private boolean isDefault = false;

    private List<UUID> statusIds;

    @Builder.Default
    private String type = "CUSTOM";

    private String statusCategoryMapping;
}