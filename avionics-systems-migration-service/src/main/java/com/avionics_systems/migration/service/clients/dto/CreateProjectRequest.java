package com.avionics_systems.migration.service.clients.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Request DTO for creating a new Project.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "Project key is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9]*$", message = "Project key must start with uppercase letter and contain only uppercase letters and numbers")
    @Size(min = 2, max = 10, message = "Project key must be between 2 and 10 characters")
    private String key;

    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 255, message = "Project name must be between 1 and 255 characters")
    private String name;

    private String description;
    private String leadUserId;
    private String projectType;
    private String projectTemplate;
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
    private String originalProjectKey;
}