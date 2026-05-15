package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTemplateResponse {
    private UUID id;
    private UUID typeId;
    private String typeName;
    private String name;
    private String description;
    private String icon;
    private String color;
    private String defaultAssigneeType;
    private Boolean allowIssueCreation;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
}