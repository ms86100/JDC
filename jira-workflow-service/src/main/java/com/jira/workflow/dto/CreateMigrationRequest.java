package com.jira.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateMigrationRequest {
    @NotNull(message = "Workflow ID is required")
    private UUID workflowId;

    @NotNull(message = "Old status ID is required")
    private UUID oldStatusId;

    @NotNull(message = "New status ID is required")
    private UUID newStatusId;

    private String migrationType;
    private UUID userId;
}