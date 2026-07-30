package com.avionics_systems.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
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

    // Extended catalog metadata
    private String categoryKey;
    private String categoryName;
    private String templateType;
    private String workflowType;
    private String workflowTypeLabel;
    private String shortDescription;
    private String iconEmoji;
    private String useCases;
    private String instructions;
    private String previewAccent;
    private Boolean recommended;
    private String projectTypeCategory;
    private List<TemplateCapabilityDto> capabilities;
}