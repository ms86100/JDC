package com.jira.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectWizardRequest {

    // Step 1: Project Type
    @NotBlank(message = "Project type is required")
    private String projectType; // COMPANY_MANAGED or TEAM_MANAGED

    // Step 2: Template
    private UUID templateId;

    // Step 3: Project Details
    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 200, message = "Project name must be between 1 and 200 characters")
    private String name;

    @NotBlank(message = "Project key is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$", message = "Project key must be 2-10 uppercase alphanumeric characters, starting with a letter")
    @Size(min = 2, max = 10, message = "Project key must be between 2 and 10 characters")
    private String projectKey;

    private UUID leadUserId;

    private String defaultAssigneeType; // PROJECT_LEAD, UNASSIGNED

    private String description;

    private String avatarUrl;

    // Step 4: Additional Options (optional)
    private Boolean allowIssueCreation = true;
}