package com.avionics_systems.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowRequest {

    private String name;

    private String description;

    private boolean isDefault;

    private List<UUID> statusIds;

    private String type;

    private String statusCategoryMapping;
}