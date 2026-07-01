package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO representing an Issue in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IssueDto {

    @EqualsAndHashCode.Include
    private String id;

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

    /**
     * Inner class representing the linked entity summary for issue links.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedIssueSummary {
        private String issueKey;
        private String summary;
        private String issueType;
    }

    /**
     * Inner class representing an issue link.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueLinkDto {
        private String id;
        private String type;
        private LinkedIssueSummary linkedIssue;
    }

    /**
     * Inner class representing attachment info embedded in issue.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentSummary {
        private String id;
        private String filename;
        private String mimeType;
        private Long size;
    }

    /**
     * Inner class representing comment summary embedded in issue.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentSummary {
        private String id;
        private String authorEmail;
        private LocalDateTime created;
        private String bodyPreview;
    }

    /**
     * Inner class representing subtask summary embedded in issue.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubtaskSummary {
        private String id;
        private String key;
        private String summary;
        private String status;
    }
}