package com.jira.project.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectImportRequest {

    private UUID projectId;

    @NotBlank(message = "{validation.project.name.required}")
    private String name;

    @NotBlank(message = "{validation.project.key.required}")
    private String projectKey;

    private String description;
    private UUID leadUserId;
    private String projectType;
    private UUID templateId;
    private String category;
    private String avatarUrl;
    private String defaultAssigneeType;
    private Boolean allowIssueCreation;

    private java.util.List<ProjectMemberExportDto> members;
    private java.util.List<ProjectRoleExportDto> roles;
    private ProjectSchemesExportDto schemes;
    private java.util.Map<String, Object> metadata;
}