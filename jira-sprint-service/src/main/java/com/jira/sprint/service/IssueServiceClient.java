package com.jira.sprint.service;

import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Client to fetch issue data from jira-issue-service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IssueServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String ISSUE_SERVICE_URL = "http://jira-issue-service:8084";

    /**
     * Fetch a single issue with story points
     */
    public IssueData getIssue(UUID issueId) {
        try {
            String url = ISSUE_SERVICE_URL + "/api/issues/" + issueId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                return mapToIssueData(response);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch issue {}: {}", issueId, e.getMessage());
        }
        return new IssueData();
    }

    /**
     * Fetch multiple issues by IDs
     */
    public List<IssueData> getIssues(List<UUID> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String ids = issueIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));
            String url = ISSUE_SERVICE_URL + "/api/issues/batch?ids=" + ids;

            Object response = restTemplate.getForObject(url, Object.class);
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> issuesList = (List<Map<String, Object>>) response;
                return issuesList.stream()
                        .map(this::mapToIssueData)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch issues: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Get story points for issues in a sprint
     */
    public int calculateSprintPoints(List<UUID> issueIds) {
        List<IssueData> issues = getIssues(issueIds);
        return issues.stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();
    }

    /**
     * Get completed story points (issues with status Done/Completed)
     */
    public int calculateCompletedPoints(List<UUID> issueIds) {
        List<IssueData> issues = getIssues(issueIds);
        return issues.stream()
                .filter(i -> isCompletedStatus(i.getStatusName()))
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();
    }

    private boolean isCompletedStatus(String status) {
        if (status == null) return false;
        String normalized = status.toLowerCase();
        return normalized.contains("done") ||
               normalized.contains("completed") ||
               normalized.contains("closed") ||
               normalized.equals("resolved");
    }

    @SuppressWarnings("unchecked")
    private IssueData mapToIssueData(Map<String, Object> response) {
        IssueData data = new IssueData();

        data.setId(parseUUID(response.get("id")));
        data.setIssueKey((String) response.getOrDefault("issueKey", response.getOrDefault("key", "")));
        data.setTitle((String) response.getOrDefault("title", ""));
        data.setStoryPoints((Integer) response.getOrDefault("storyPoints", 0));

        // Extract status
        Object statusObj = response.getOrDefault("statusName", response.get("status"));
        if (statusObj instanceof String) {
            data.setStatusName((String) statusObj);
        }

        // Extract priority
        Object priorityObj = response.getOrDefault("priorityName", response.get("priority"));
        if (priorityObj instanceof String) {
            data.setPriorityName((String) priorityObj);
        }

        // Extract issue type
        Object typeObj = response.getOrDefault("issueTypeName", response.get("issueType"));
        if (typeObj instanceof String) {
            data.setIssueTypeName((String) typeObj);
        }

        // Extract assignee
        Object assigneeId = response.get("assigneeId");
        if (assigneeId != null) {
            data.setAssigneeId(parseUUID(assigneeId));
        }

        // Extract reporter
        Object reporterId = response.get("reporterId");
        if (reporterId != null) {
            data.setReporterId(parseUUID(reporterId));
        }

        return data;
    }

    private UUID parseUUID(Object obj) {
        if (obj == null) return null;
        if (obj instanceof UUID) return (UUID) obj;
        try {
            return UUID.fromString(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    public static class IssueData {
        private UUID id;
        private String issueKey;
        private String title;
        private Integer storyPoints;
        private String statusName;
        private String priorityName;
        private String issueTypeName;
        private UUID assigneeId;
        private UUID reporterId;
        private Date createdAt;
        private Date updatedAt;
    }
}
