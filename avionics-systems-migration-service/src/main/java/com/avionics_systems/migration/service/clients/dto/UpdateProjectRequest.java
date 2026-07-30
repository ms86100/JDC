package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

import java.util.List;

/**
 * Request DTO for updating an existing Project.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectRequest {

    private String name;
    private String description;
    private String leadUserId;
    private String avatarUrl;
    private String issueSecurityScheme;
    private String notificationScheme;
    private String permissionScheme;
    private String workflowScheme;
    private List<String> defaultIssueTypeIds;
    private List<String> defaultPriorityIds;
    private List<String> defaultComponentIds;
    private List<String> defaultVersionIds;
    private String url;
    private String email;
    private String category;
    private Boolean archived;
}