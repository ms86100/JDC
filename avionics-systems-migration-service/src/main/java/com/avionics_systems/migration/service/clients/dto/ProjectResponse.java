package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Project operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectResponse {

    @EqualsAndHashCode.Include
    private String id;

    private String key;
    private String name;
    private String description;
    private String leadUserId;
    private String leadUsername;
    private String leadEmail;
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
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime archivedDate;
    private boolean archived;
    private boolean deleted;
    private String url;
    private String email;
    private Integer issueCount;
    private String category;
    private boolean success;
    private String errorMessage;
    private String originalProjectKey;
}