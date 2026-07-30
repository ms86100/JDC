package com.avionics_systems.issue.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponse {

    private UUID id;
    private UUID projectId;
    private String projectKey;
    private String projectName;
    private String issueKey;
    private String title;
    private String description;
    private String environment;

    // Issue Type & Status
    private UUID statusId;
    private String statusName;
    private String statusCategory;
    private UUID issueTypeId;
    private String issueTypeName;
    private String issueTypeIcon;
    private String issueTypeColor;

    // Priority
    private UUID priorityId;
    private String priorityName;
    private String priorityColor;

    // Resolution
    private UUID resolutionId;
    private String resolutionName;
    private LocalDateTime resolutionDate;

    // User Relationships (People section)
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatar;
    private UUID reporterId;
    private String reporterName;
    private String reporterAvatar;
    private UUID creatorId;
    private String creatorName;

    // Hierarchy
    private UUID parentIssueId;
    private String parentIssueKey;
    private Boolean subTask;
    private Integer subTaskCount;

    // Epic fields (Agile section)
    private UUID epicId;
    private String epicName;
    private String epicColor;

    // Security level
    private UUID securityLevelId;
    private String securityLevelName;

    // Versions (sidebar)
    private UUID[] affectsVersions;
    private String[] affectsVersionNames;
    private UUID[] fixVersions;
    private String[] fixVersionNames;

    // Story points, business value, and rank (Agile section)
    private Integer storyPoints;
    private Integer businessValue;
    private String rank;

    // Time tracking (sidebar)
    private Long originalEstimate;
    private Long remainingEstimate;
    private Long timeSpent;
    private Double workRatio;
    private Long aggregateTimeEstimate;
    private Long aggregateTimeSpent;

    // Organization (sidebar details)
    private UUID[] componentIds;
    private String[] componentNames;
    private String[] labels;

    // Sprint (Agile section)
    private UUID sprintId;
    private String sprintName;
    private String sprintState;

    // Team
    private UUID teamId;
    private String teamName;

    // Due date (Dates section)
    private LocalDate dueDate;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastViewedAt;
    private Long version;

    // Votes and watchers (People section)
    private Integer voteCount;
    private Integer watcherCount;
    private List<UserInfo> watchers;
    private Boolean voted;
    private Boolean watching;

    // Linked issues
    private List<LinkedIssueInfo> linkedIssues;

    // Custom fields (dynamic - for plugins and custom fields)
    private Map<String, Object> customFields;

    // Field definitions for dynamic rendering
    private Map<String, FieldDefinitionInfo> fieldDefinitions;

    // Nested data
    private List<IssueResponse> subtasks;
    private IssueResponse parent;

    // ========== Nested Types ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String name;
        private String email;
        private String avatar;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedIssueInfo {
        private String linkType;
        private UUID issueId;
        private String issueKey;
        private String title;
        private String direction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDefinitionInfo {
        private UUID id;
        private String fieldKey;
        private String displayName;
        private String fieldType;
        private String renderer;
        private String screenRegion;
        private Boolean searchable;
        private Boolean sortable;
        private Boolean required;
        private Boolean custom;
    }
}
