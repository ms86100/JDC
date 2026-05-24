package com.jira.project.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSchemesExportDto {

    private UUID issueTypeSchemeId;
    private UUID workflowSchemeId;
    private UUID permissionSchemeId;
    private UUID notificationSchemeId;
    private UUID screenSchemeId;
}