package com.avionics_systems.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDetailsResponse {
    private UUID templateId;
    private String templateName;
    private String icon;
    private String color;
    private String defaultAssigneeType;
    private Boolean allowIssueCreation;

    // Scheme IDs that will be assigned
    private UUID issueTypeSchemeId;
    private String issueTypeSchemeName;
    private UUID workflowSchemeId;
    private String workflowSchemeName;
    private UUID permissionSchemeId;
    private String permissionSchemeName;
    private UUID notificationSchemeId;
    private String notificationSchemeName;
    private UUID screenSchemeId;
    private String screenSchemeName;

    // Default roles to create
    private String[] defaultRoles;
}