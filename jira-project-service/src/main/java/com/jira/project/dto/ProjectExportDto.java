package com.jira.project.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectExportDto {

    private UUID projectId;
    private String projectKey;
    private String name;
    private String description;
    private UUID leadUserId;
    private String projectType;
    private UUID templateId;
    private String category;
    private String avatarUrl;
    private String defaultAssigneeType;
    private Boolean allowIssueCreation;
    private LocalDateTime exportedAt;

    private List<ProjectMemberExportDto> members;
    private List<ProjectRoleExportDto> roles;
    private ProjectSchemesExportDto schemes;
    private Map<String, Object> metadata;
}