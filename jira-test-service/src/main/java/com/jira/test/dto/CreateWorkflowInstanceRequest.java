package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkflowInstanceRequest {

    @NotNull(message = "Definition ID is required")
    private UUID definitionId;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    private UUID entityId;

    private UUID initiatedBy;

    private UUID assignedTo;
}