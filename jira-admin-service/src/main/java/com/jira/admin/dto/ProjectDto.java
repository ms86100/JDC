package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectDto {
    private String id;
    private String projectKey;
    private String name;
    private String description;
    private String type;
    private String status;
    private String leadUserId;
    private String defaultAssignee;
    private String defaultPriority;
    private String defaultIssueType;
    private Boolean allowSubTasks;
    private Boolean allowAttachments;
    private Boolean allowComments;
    private Integer maxAttachments;
    private String workflowScheme;
    private String issueTypeScheme;
    private String permissionSchemeId;
    private Boolean enableNotifications;
    private String category;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}