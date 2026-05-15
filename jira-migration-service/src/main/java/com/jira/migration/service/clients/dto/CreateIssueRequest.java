package com.jira.migration.service.clients.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new Issue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIssueRequest {

    @NotBlank(message = "Summary is required")
    private String summary;

    @NotBlank(message = "Project ID is required")
    private String projectId;

    @NotBlank(message = "Issue type is required")
    private String issueType;

    private String description;
    private String status;
    private String priority;
    private String assigneeId;
    private String reporterId;
    private LocalDateTime dueDate;
    private Double storyPoints;
    private String sprintId;
    private String epicId;
    private String parentId;
    private List<String> labels;
    private List<String> components;
    private List<String> fixVersions;
    private List<String> affectedVersions;
    private Map<String, Object> customFields;
    private String securityLevel;
    private String environment;
    private String originalIssueKey;
    private Map<String, String> fieldMappings;
}