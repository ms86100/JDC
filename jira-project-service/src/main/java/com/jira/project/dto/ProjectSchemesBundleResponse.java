package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSchemesBundleResponse {
    private UUID projectId;
    private UUID projectSchemeId;
    private UUID issueTypeSchemeId;
    private UUID workflowSchemeId;
    private UUID permissionSchemeId;
    private UUID notificationSchemeId;
    private UUID screenSchemeId;
    private UUID fieldConfigurationSchemeId;
}
