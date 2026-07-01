package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced template response with workflow visualization data
 * This DTO is used for displaying template selection and workflow preview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateWithWorkflowResponse {
    private UUID id;
    private UUID typeId;
    private String typeName;
    private String name;
    private String description;
    private String icon;
    private String color;
    private String category;
    private String templateType;
    private String defaultAssigneeType;
    private Boolean allowIssueCreation;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String instructions;

    // Issue types for this template
    private List<TemplateIssueTypeDto> issueTypes;

    // Workflow statuses
    private List<TemplateWorkflowStatusDto> workflowStatuses;

    // Workflow transitions
    private List<TemplateWorkflowTransitionDto> workflowTransitions;

    // Scheme information
    private SchemeInfoDto issueTypeScheme;
    private SchemeInfoDto workflowScheme;
    private SchemeInfoDto permissionScheme;
    private SchemeInfoDto notificationScheme;
    private SchemeInfoDto screenScheme;

    /**
     * Issue type info for template
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateIssueTypeDto {
        private UUID id;
        private String issueTypeName;
        private String issueTypeIcon;
        private Boolean isDefault;
        private Boolean isSubtask;
        private Integer sequence;
    }

    /**
     * Workflow status info for template visualization
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateWorkflowStatusDto {
        private UUID id;
        private String statusName;
        private String statusKey;
        private String statusColor;
        private String statusCategory;
        private Integer sequence;
        private String description;
        private String icon;
    }

    /**
     * Workflow transition info for template visualization
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateWorkflowTransitionDto {
        private UUID id;
        private String fromStatusKey;
        private String toStatusKey;
        private String transitionName;
        private String transitionIcon;
        private Boolean allowBackward;
        private Boolean requiresApproval;
        private Integer sequence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchemeInfoDto {
        private UUID id;
        private String name;
    }
}