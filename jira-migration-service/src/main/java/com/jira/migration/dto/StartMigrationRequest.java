package com.jira.migration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartMigrationRequest {
    @NotNull(message = "Job type is required")
    private String jobType; // IMPORT, EXPORT

    private String importSource; // JIRA_DC, CSV, BACKUP

    private UUID sourceProjectId;
    private UUID targetProjectId;
    private UUID templateId;

    private Map<String, Object> config;
    private Map<String, Object> options;
}