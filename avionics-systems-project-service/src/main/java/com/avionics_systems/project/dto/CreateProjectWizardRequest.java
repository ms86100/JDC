package com.avionics_systems.project.dto;

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
    @NotBlank(message = "{validation.project.type.required}")
    private String projectType; // COMPANY_MANAGED or TEAM_MANAGED

    // Step 2: Template
    private UUID templateId;

    // Step 3: Project Details
    @NotBlank(message = "{validation.project.name.required}")
    @Size(min = 1, max = 200, message = "{validation.project.name.size}")
    private String name;

    @NotBlank(message = "{validation.project.key.required}")
    @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$", message = "{validation.project.key.format}")
    @Size(min = 2, max = 10, message = "{validation.project.key.size}")
    private String projectKey;

    private UUID leadUserId;

    private String defaultAssigneeType; // PROJECT_LEAD, UNASSIGNED

    private String description;

    private String avatarUrl;

    // Step 4: Additional Options (optional)
    private Boolean allowIssueCreation = true;
}