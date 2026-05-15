package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSchemeResponse {
    private UUID id;
    private UUID projectId;
    private IssueTypeSchemeInfo issueTypeScheme;
    private WorkflowSchemeInfo workflowScheme;
    private PermissionSchemeInfo permissionScheme;
    private NotificationSchemeInfo notificationScheme;
    private ScreenSchemeInfo screenScheme;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueTypeSchemeInfo {
        private UUID id;
        private String name;
        private String[] issueTypeNames; // Using names since issue types are in different DB
        private String defaultIssueTypeName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowSchemeInfo {
        private UUID id;
        private String name;
        private String defaultWorkflowName; // Using name since workflows are in different DB
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionSchemeInfo {
        private UUID id;
        private String name;
        private String permissions; // JSON
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSchemeInfo {
        private UUID id;
        private String name;
        private String notifications; // JSON
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScreenSchemeInfo {
        private UUID id;
        private String name;
        private ScreenMappingInfo[] screens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScreenMappingInfo {
        private String screenType;
        private UUID screenId;
    }
}