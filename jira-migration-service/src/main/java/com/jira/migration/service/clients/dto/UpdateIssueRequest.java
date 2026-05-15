package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating an existing Issue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueRequest {

    private String summary;
    private String description;
    private String status;
    private String priority;
    private String assigneeId;
    private String reporterId;
    private LocalDateTime dueDate;
    private Double storyPoints;
    private String sprintId;
    private String epicId;
    private List<String> labels;
    private List<String> components;
    private List<String> fixVersions;
    private List<String> affectedVersions;
    private Map<String, Object> customFields;
    private String securityLevel;
    private String environment;
    private String resolution;
    private LocalDateTime resolved;

    /**
     * Whether to send notifications for this update.
     */
    private Boolean notifyUsers;
}