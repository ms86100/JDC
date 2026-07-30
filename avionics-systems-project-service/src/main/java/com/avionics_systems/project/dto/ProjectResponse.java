package com.avionics_systems.project.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;
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
    private Boolean archived;
    private LocalDateTime archivedAt;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}