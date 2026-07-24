package com.jira.workflow.engine;

import com.jira.workflow.config.PatchCapableRestTemplate;
import com.jira.workflow.dto.ExecuteTransitionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowIntegrationClient {

    private final PatchCapableRestTemplate patchCapableRestTemplate;

    public RestTemplate restTemplate() {
        return patchCapableRestTemplate.get();
    }

    @Value("${jira.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    public String getIssueServiceUrl() { return issueServiceUrl; }

    @Value("${jira.services.notification-url:http://jira-notification-service:8087}")
    private String notificationServiceUrl;

    public String getNotificationServiceUrl() { return notificationServiceUrl; }

    @Value("${jira.services.comment-url:http://localhost:8086}")
    private String commentServiceUrl;

    @Value("${jira.services.attachment-url:http://localhost:8090}")
    private String attachmentServiceUrl;

    @Value("${jira.services.user-url:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${jira.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Value("${jira.services.search-url:http://localhost:8088}")
    private String searchServiceUrl;

    @Value("${jira.services.version-url:http://jira-version-service:8096}")
    private String versionServiceUrl;

    @Value("${jira.services.component-url:http://jira-component-service:8097}")
    private String componentServiceUrl;

    @Value("${jira.services.test-url:http://jira-test-service:8095}")
    private String testServiceUrl;

    /**
     * Fetch issue data from issue-service first, then fall back to test-service.
     * Test-service hosts VVO, HLVVO, TechEvent, BenchDefect, ProblemReport entities
     * which also expose {@code /api/issues/{id}} via WorkflowInternalController.
     */
    public Map<String, Object> fetchIssue(UUID issueId) {
        // Try issue-service first
        try {
            Map<?, ?> response = restTemplate().getForObject(issueServiceUrl + "/api/issues/" + issueId, Map.class);
            if (response != null && !response.isEmpty()) {
                return castMap(response);
            }
        } catch (Exception e) {
            log.debug("Issue {} not found in issue-service, trying test-service: {}", issueId, e.getMessage());
        }

        // Fall back to test-service (VVO, HLVVO, TechEvent, BenchDefect, ProblemReport)
        try {
            Map<?, ?> response = restTemplate().getForObject(testServiceUrl + "/api/issues/" + issueId, Map.class);
            if (response != null && !response.isEmpty()) {
                return castMap(response);
            }
        } catch (Exception e) {
            log.error("Failed to fetch issue {} from both issue-service and test-service: {}", issueId, e.getMessage());
        }

        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchIssueStatuses() {
        try {
            List<?> response = restTemplate().getForObject(issueServiceUrl + "/api/issues/statuses", List.class);
            if (response == null) {
                return List.of();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) {
                    result.add(castMap(m));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch issue statuses: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> fetchUser(UUID userId) {
        if (userId == null) {
            return new HashMap<>();
        }
        try {
            Map<?, ?> response = restTemplate().getForObject(userServiceUrl + "/api/users/" + userId, Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch user {}: {}", userId, e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchLinkedIssuesForWorkflow(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/links/workflow-context",
                    List.class);
            if (response == null) {
                return List.of();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) {
                    result.add(castMap(m));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch linked issues for {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> fetchProject(UUID projectId) {
        if (projectId == null) {
            return new HashMap<>();
        }
        try {
            Map<?, ?> response = restTemplate().getForObject(projectServiceUrl + "/api/projects/" + projectId, Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch project {}: {}", projectId, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Update issue status via internal workflow endpoint.
     * Tries issue-service first, then falls back to test-service for V&V entities.
     */
    public void updateIssueWorkflowInternal(UUID issueId, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Workflow-Internal", "true");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // Try issue-service first
        try {
            String url = issueServiceUrl + "/api/issues/" + issueId + "/workflow/internal";
            restTemplate().exchange(url, HttpMethod.PATCH, entity, Map.class);
            return;
        } catch (Exception e) {
            log.debug("Issue {} not in issue-service for workflow update, trying test-service: {}",
                    issueId, e.getMessage());
        }

        // Fall back to test-service
        try {
            String url = testServiceUrl + "/api/issues/" + issueId + "/workflow/internal";
            restTemplate().exchange(url, HttpMethod.PATCH, entity, Map.class);
        } catch (Exception e) {
            log.error("Failed to update issue {} via both issue-service and test-service: {}",
                    issueId, e.getMessage());
        }
    }

    public void updateIssueStatusInternal(UUID issueId, UUID projectId, UUID statusId, Map<String, Object> extra) {
        Map<String, Object> body = new HashMap<>();
        body.put("statusId", statusId.toString());
        if (extra != null) {
            body.putAll(extra);
        }
        updateIssueWorkflowInternal(issueId, body);
    }

    public void patchIssueFields(UUID issueId, Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        updateIssueWorkflowInternal(issueId, fields);
    }

    public int countAttachments(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    attachmentServiceUrl + "/api/attachments/issue/" + issueId,
                    List.class);
            return response != null ? response.size() : 0;
        } catch (Exception e) {
            log.warn("Could not count attachments for issue {}: {}", issueId, e.getMessage());
            return 0;
        }
    }

    /**
     * Record transition history. Tries issue-service, then test-service.
     */
    public void recordIssueTransitionHistory(
            UUID issueId,
            UUID projectId,
            UUID workflowId,
            UUID transitionId,
            String transitionName,
            UUID fromStatusId,
            UUID toStatusId,
            UUID userId,
            String comment,
            boolean success,
            String errorMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Workflow-Internal", "true");
        Map<String, Object> body = new HashMap<>();
        body.put("projectId", projectId != null ? projectId.toString() : null);
        body.put("workflowId", workflowId != null ? workflowId.toString() : null);
        body.put("transitionId", transitionId != null ? transitionId.toString() : null);
        body.put("transitionName", transitionName);
        body.put("fromStatusId", fromStatusId != null ? fromStatusId.toString() : null);
        body.put("toStatusId", toStatusId != null ? toStatusId.toString() : null);
        body.put("userId", userId != null ? userId.toString() : null);
        body.put("comment", comment);
        body.put("success", success);
        body.put("errorMessage", errorMessage);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String historyPath = "/api/issues/" + issueId + "/transitions/history/internal";

        // Try issue-service first
        try {
            restTemplate().postForObject(issueServiceUrl + historyPath, entity, Map.class);
            return;
        } catch (Exception e) {
            log.debug("Issue {} transition history not recorded in issue-service, trying test-service", issueId);
        }

        // Fall back to test-service
        try {
            restTemplate().postForObject(testServiceUrl + historyPath, entity, Map.class);
        } catch (Exception e) {
            log.warn("Could not record issue transition history for {} in either service: {}",
                    issueId, e.getMessage());
        }
    }

    public void fireWebhook(String url, Map<String, Object> payload) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate().postForObject(url, new HttpEntity<>(payload, headers), Map.class);
        } catch (Exception e) {
            log.warn("Webhook call failed for {}: {}", url, e.getMessage());
        }
    }

    public void addComment(UUID issueId, String content, UUID userId) {
        if (content == null || content.isBlank()) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) {
                headers.set("X-User-Id", userId.toString());
            }
            Map<String, Object> body = Map.of("issueId", issueId.toString(), "content", content);
            restTemplate().postForObject(
                    commentServiceUrl + "/api/comments",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Could not add transition comment: {}", e.getMessage());
        }
    }

    public void recordChangeHistory(UUID issueId, UUID authorId, String authorName, List<Map<String, Object>> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Workflow-Internal", "true");
            Map<String, Object> body = new HashMap<>();
            body.put("authorId", authorId != null ? authorId.toString() : null);
            body.put("authorName", authorName);
            body.put("changes", changes);
            restTemplate().postForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/history/internal",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Could not record change history for issue {}: {}", issueId, e.getMessage());
        }
    }

    public void indexIssue(UUID issueId, String title, String content) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("entityType", "issue");
            body.put("entityId", issueId.toString());
            body.put("title", title);
            body.put("content", content);
            restTemplate().postForObject(
                    searchServiceUrl + "/api/search/index",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Search index failed for issue {}: {}", issueId, e.getMessage());
        }
    }

    public void createIssueLink(UUID sourceIssueId, UUID targetIssueId, UUID linkTypeId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("sourceIssueId", sourceIssueId.toString());
            body.put("targetIssueId", targetIssueId.toString());
            body.put("linkTypeId", linkTypeId.toString());
            restTemplate().postForObject(
                    issueServiceUrl + "/api/issues/links",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Create issue link failed: {}", e.getMessage());
        }
    }

    public void createSubtask(WorkflowContext ctx, Map<String, Object> config) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (ctx.getUserId() != null) {
                headers.set("X-User-Id", ctx.getUserId().toString());
            }
            Map<String, Object> body = new HashMap<>();
            body.put("projectId", ctx.getProjectId().toString());
            body.put("parentIssueId", ctx.getIssueId().toString());
            body.put("title", config.getOrDefault("summary", "Sub-task"));
            if (config.get("issueTypeId") != null) {
                body.put("issueTypeId", config.get("issueTypeId").toString());
            }
            if (ctx.getIssueData().get("reporterId") != null) {
                body.put("reporterId", ctx.getIssueData().get("reporterId").toString());
            }
            restTemplate().postForObject(
                    issueServiceUrl + "/api/issues",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Create subtask post-function failed: {}", e.getMessage());
        }
    }

    /**
     * Checks if a user has a specific permission in a project.
     *
     * @param userId     The user to check
     * @param projectId  The project to check permission in
     * @param permission The permission to check (e.g., "EDIT_ISSUES", "ASSIGN_ISSUES")
     * @return true if the user has the permission
     */
    public boolean checkUserPermission(UUID userId, UUID projectId, String permission) {
        if (userId == null || projectId == null || permission == null) {
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                    "userId", userId.toString(),
                    "projectId", projectId.toString(),
                    "permission", permission);
            Map<?, ?> response = restTemplate().postForObject(
                    projectServiceUrl + "/api/projects/permissions/check",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response != null) {
                Object result = response.get("hasPermission");
                if (result instanceof Boolean b) {
                    return b;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("Permission check failed for user {} in project {}: {}", userId, projectId, e.getMessage());
            return false;
        }
    }

    public Map<String, Object> fetchIssueByKey(String issueKey) {
        if (issueKey == null || !issueKey.matches("^[A-Za-z][A-Za-z0-9_]+-\\d+$")) {
            log.warn("Invalid issue key format: {}", issueKey);
            return new HashMap<>();
        }
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/by-key/" + issueKey, Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch issue by key {}: {}", issueKey, e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchComments(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    commentServiceUrl + "/api/comments/issue/" + issueId, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch comments for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchIssueHistory(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/history", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch history for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchWatchers(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/watchers", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch watchers for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    public void addWatcher(UUID issueId, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) headers.set("X-User-Id", userId.toString());
            restTemplate().postForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/watchers",
                    new HttpEntity<>(Map.of("userId", userId.toString()), headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Failed to add watcher to issue {}: {}", issueId, e.getMessage());
        }
    }

    public Map<String, Object> fetchProjectByKey(String projectKey) {
        if (projectKey == null || !projectKey.matches("^[A-Za-z][A-Za-z0-9_-]{0,30}$")) {
            log.warn("Invalid project key format: {}", projectKey);
            return new HashMap<>();
        }
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    projectServiceUrl + "/api/projects/key/" + projectKey, Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch project by key {}: {}", projectKey, e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchProjectVersions(UUID projectId) {
        try {
            List<?> response = restTemplate().getForObject(
                    versionServiceUrl + "/api/versions/project/" + projectId, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch versions for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchProjectComponents(UUID projectId) {
        try {
            List<?> response = restTemplate().getForObject(
                    componentServiceUrl + "/api/components/project/" + projectId, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch components for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchIssueTypes() {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/types", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch issue types: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchIssuesJql(String jql, int maxResults) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("jql", jql);
            body.put("maxResults", maxResults);
            Map<?, ?> response = restTemplate().postForObject(
                    issueServiceUrl + "/api/jql/search",
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response != null && response.get("issues") instanceof List<?> issues) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : issues) {
                    if (item instanceof Map<?, ?> m) result.add(castMap(m));
                }
                return result;
            }
            return List.of();
        } catch (Exception e) {
            log.warn("JQL search failed for '{}': {}", jql, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchProjectMembers(UUID projectId) {
        try {
            List<?> response = restTemplate().getForObject(
                    projectServiceUrl + "/api/projects/" + projectId + "/members", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch members for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    // === Issue Mutation Methods ===

    public Map<String, Object> createIssue(Map<String, Object> issueData, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) headers.set("X-User-Id", userId.toString());
            Map<?, ?> response = restTemplate().postForObject(
                    issueServiceUrl + "/api/issues",
                    new HttpEntity<>(issueData, headers), Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to create issue: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> cloneIssue(UUID issueId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<?, ?> response = restTemplate().postForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/clone",
                    new HttpEntity<>(Map.of(), headers), Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to clone issue {}: {}", issueId, e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> moveIssue(UUID issueId, UUID targetProjectId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<?, ?> response = restTemplate().postForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/move",
                    new HttpEntity<>(Map.of("targetProjectId", targetProjectId.toString()), headers), Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to move issue {}: {}", issueId, e.getMessage());
            return new HashMap<>();
        }
    }

    public void deleteIssue(UUID issueId) {
        try {
            restTemplate().delete(issueServiceUrl + "/api/issues/" + issueId);
        } catch (Exception e) {
            log.warn("Failed to delete issue {}: {}", issueId, e.getMessage());
        }
    }

    public void transitionIssue(UUID issueId, UUID projectId, String transitionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Workflow-Internal", "true");
            Map<String, Object> body = new HashMap<>();
            body.put("transitionId", transitionId);
            restTemplate().exchange(
                    issueServiceUrl + "/api/issues/" + issueId + "/status?projectId=" + projectId,
                    HttpMethod.PATCH, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to transition issue {}: {}", issueId, e.getMessage());
        }
    }

    // === Label Methods ===

    public void addLabel(UUID issueId, String label) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate().postForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/labels",
                    new HttpEntity<>(Map.of("name", label), headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to add label to issue {}: {}", issueId, e.getMessage());
        }
    }

    public void removeLabel(UUID issueId, String label) {
        try {
            restTemplate().delete(issueServiceUrl + "/api/issues/" + issueId + "/labels/" + label);
        } catch (Exception e) {
            log.warn("Failed to remove label from issue {}: {}", issueId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchLabels(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/labels", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
                else result.add(Map.of("name", String.valueOf(item)));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch labels for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    // === Worklog Methods ===

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchWorklogs(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/worklogs", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch worklogs for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> addWorklog(UUID issueId, String timeSpent, String comment, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) headers.set("X-User-Id", userId.toString());
            Map<String, Object> body = new HashMap<>();
            body.put("timeSpent", timeSpent);
            if (comment != null) body.put("comment", comment);
            Map<?, ?> response = restTemplate().postForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/worklogs",
                    new HttpEntity<>(body, headers), Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to add worklog to issue {}: {}", issueId, e.getMessage());
            return new HashMap<>();
        }
    }

    // === Subtask Methods ===

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchSubtasks(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/hierarchy/" + issueId + "/subtasks", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch subtasks for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    // === Vote Methods ===

    public void addVote(UUID issueId, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) headers.set("X-User-Id", userId.toString());
            restTemplate().postForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/votes",
                    new HttpEntity<>(Map.of(), headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to add vote to issue {}: {}", issueId, e.getMessage());
        }
    }

    public void removeVote(UUID issueId, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (userId != null) headers.set("X-User-Id", userId.toString());
            restTemplate().exchange(
                    issueServiceUrl + "/api/issues/" + issueId + "/votes",
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) {
            log.warn("Failed to remove vote from issue {}: {}", issueId, e.getMessage());
        }
    }

    public void removeWatcher(UUID issueId, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (userId != null) headers.set("X-User-Id", userId.toString());
            restTemplate().exchange(
                    issueServiceUrl + "/api/issues/" + issueId + "/watchers",
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) {
            log.warn("Failed to remove watcher from issue {}: {}", issueId, e.getMessage());
        }
    }

    // === Version/Component Write Methods ===

    public Map<String, Object> createVersion(Map<String, Object> versionData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<?, ?> response = restTemplate().postForObject(
                    versionServiceUrl + "/api/versions",
                    new HttpEntity<>(versionData, headers), Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to create version: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public void releaseVersion(UUID versionId) {
        try {
            restTemplate().postForObject(
                    versionServiceUrl + "/api/versions/" + versionId + "/release",
                    new HttpEntity<>(Map.of()), Map.class);
        } catch (Exception e) {
            log.warn("Failed to release version {}: {}", versionId, e.getMessage());
        }
    }

    public Map<String, Object> createComponent(Map<String, Object> componentData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<?, ?> response = restTemplate().postForObject(
                    componentServiceUrl + "/api/components",
                    new HttpEntity<>(componentData, headers), Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to create component: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
}
