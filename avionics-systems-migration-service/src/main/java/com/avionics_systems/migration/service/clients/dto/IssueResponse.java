package com.avionics_systems.migration.service.clients.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for Issue operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IssueResponse {

    @EqualsAndHashCode.Include
    private String id;

    /** issue-service serializes this as {@code issueKey}. */
    @JsonProperty("issueKey")
    private String key;
    private String summary;
    private String description;
    private String issueType;
    private String status;
    private String priority;
    private String projectId;
    private String projectKey;
    private String assigneeId;
    private String assigneeEmail;
    private String reporterId;
    private String reporterEmail;
    private String resolution;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime dueDate;
    private LocalDateTime resolved;
    private Integer votes;
    private Integer watches;
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
    private boolean success;
    private String errorMessage;
    private String originalIssueKey;
}