package com.jira.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateMigrationRequest {
    @NotNull(message = "{validation.migration.workflow.required}")
    private UUID workflowId;

    @NotNull(message = "{validation.migration.old.status.required}")
    private UUID oldStatusId;

    @NotNull(message = "{validation.migration.new.status.required}")
    private UUID newStatusId;

    private String migrationType;
    private UUID userId;
}