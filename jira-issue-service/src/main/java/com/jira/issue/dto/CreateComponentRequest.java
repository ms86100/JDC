package com.jira.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComponentRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Component name is required")
    private String name;

    private String description;

    private UUID leadId;

    private String assigneeType;

    private Boolean isAssigneeTypeEnabled;

    private UUID defaultAssigneeId;
}