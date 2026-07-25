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
import java.util.Base64;
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

    @Value("${jira.services.plan-url:http://jira-plan-service:8098}")
    private String planServiceUrl;

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

    // === Attachment Methods ===

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchAttachments(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    attachmentServiceUrl + "/api/attachments/issue/" + issueId, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch attachments for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    public byte[] fetchAttachmentContent(UUID attachmentId) {
        try {
            return restTemplate().getForObject(
                    attachmentServiceUrl + "/api/attachments/" + attachmentId + "/download",
                    byte[].class);
        } catch (Exception e) {
            log.warn("Failed to fetch attachment content {}: {}", attachmentId, e.getMessage());
            return new byte[0];
        }
    }

    public void deleteAttachment(UUID attachmentId) {
        try {
            restTemplate().delete(attachmentServiceUrl + "/api/attachments/" + attachmentId);
        } catch (Exception e) {
            log.warn("Failed to delete attachment {}: {}", attachmentId, e.getMessage());
        }
    }

    public String getAttachmentUrl(UUID attachmentId) {
        return attachmentServiceUrl + "/api/attachments/" + attachmentId + "/download";
    }

    public void copyAttachments(UUID sourceIssueId, UUID targetIssueId) {
        try {
            List<Map<String, Object>> attachments = fetchAttachments(sourceIssueId);
            for (Map<String, Object> att : attachments) {
                Object attId = att.get("id");
                if (attId == null) continue;
                byte[] content = fetchAttachmentContent(UUID.fromString(attId.toString()));
                if (content == null || content.length == 0) continue;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                Map<String, Object> meta = new HashMap<>();
                meta.put("issueId", targetIssueId.toString());
                meta.put("fileName", att.getOrDefault("fileName", "attachment"));
                meta.put("contentType", att.getOrDefault("contentType", "application/octet-stream"));
                HttpHeaders uploadHeaders = new HttpHeaders();
                uploadHeaders.setContentType(MediaType.APPLICATION_JSON);
                Map<String, Object> body = new HashMap<>(meta);
                body.put("data", Base64.getEncoder().encodeToString(content));
                restTemplate().postForObject(
                        attachmentServiceUrl + "/api/attachments",
                        new HttpEntity<>(body, uploadHeaders), Map.class);
            }
        } catch (Exception e) {
            log.warn("Failed to copy attachments from issue {} to {}: {}",
                    sourceIssueId, targetIssueId, e.getMessage());
        }
    }

    public Map<String, Object> uploadAttachment(UUID issueId, String filename, String base64Content) {
        try {
            byte[] content = java.util.Base64.getDecoder().decode(base64Content);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("issueId", issueId.toString());
            body.add("uploaderName", "script-engine");
            org.springframework.core.io.ByteArrayResource fileResource = new org.springframework.core.io.ByteArrayResource(content) {
                @Override
                public String getFilename() { return filename; }
            };
            body.add("file", fileResource);
            Map<?, ?> response = restTemplate().postForObject(
                    attachmentServiceUrl + "/api/attachments",
                    new org.springframework.http.HttpEntity<>(body, headers), Map.class);
            return response != null ? castMap(response) : new java.util.HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to upload attachment: {}", e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    // === Comment Mutation Methods ===

    public void deleteComment(UUID commentId) {
        try {
            restTemplate().delete(commentServiceUrl + "/api/comments/" + commentId);
        } catch (Exception e) {
            log.warn("Failed to delete comment {}: {}", commentId, e.getMessage());
        }
    }

    public void updateComment(UUID commentId, String newText, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) headers.set("X-User-Id", userId.toString());
            Map<String, Object> body = Map.of("content", newText);
            restTemplate().put(
                    commentServiceUrl + "/api/comments/" + commentId,
                    new HttpEntity<>(body, headers));
        } catch (Exception e) {
            log.warn("Failed to update comment {}: {}", commentId, e.getMessage());
        }
    }

    // === User/Group Management (Task 2.4) ===

    public void addUserToGroup(UUID userId, String groupName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate().postForObject(
                    userServiceUrl + "/api/admin/groups/" + groupName + "/members",
                    new HttpEntity<>(Map.of("userId", userId.toString()), headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to add user {} to group {}: {}", userId, groupName, e.getMessage());
        }
    }

    public void removeUserFromGroup(UUID userId, String groupName) {
        try {
            restTemplate().exchange(
                    userServiceUrl + "/api/admin/groups/" + groupName + "/members/" + userId,
                    HttpMethod.DELETE, new HttpEntity<>(new HttpHeaders()), Void.class);
        } catch (Exception e) {
            log.warn("Failed to remove user {} from group {}: {}", userId, groupName, e.getMessage());
        }
    }

    public boolean checkUserIsAdmin(UUID userId) {
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    userServiceUrl + "/api/admin/users/" + userId, Map.class);
            if (response != null) {
                Object role = response.get("role");
                return "ADMIN".equalsIgnoreCase(String.valueOf(role));
            }
            return false;
        } catch (Exception e) {
            log.warn("Failed to check admin status for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public Map<String, Object> fetchUserByEmail(String email) {
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    userServiceUrl + "/api/admin/users?email=" + email, Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch user by email {}: {}", email, e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchUsers(String query, int limit) {
        try {
            String url = userServiceUrl + "/api/admin/users?search=" + (query != null ? query : "") + "&size=" + limit;
            List<?> response = restTemplate().getForObject(url, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to search users: {}", e.getMessage());
            return List.of();
        }
    }

    // === Field Metadata (Task 2.5) ===

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchAvailableFieldValues(String fieldName, UUID projectId, UUID issueTypeId) {
        try {
            StringBuilder url = new StringBuilder(issueServiceUrl + "/api/issues/fields/" + fieldName + "/values");
            String sep = "?";
            if (projectId != null) { url.append(sep).append("projectId=").append(projectId); sep = "&"; }
            if (issueTypeId != null) { url.append(sep).append("issueTypeId=").append(issueTypeId); }
            List<?> response = restTemplate().getForObject(url.toString(), List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch available field values for {}: {}", fieldName, e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> fetchFieldMetadata(String fieldName) {
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/fields/" + fieldName + "/metadata", Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch field metadata for {}: {}", fieldName, e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchCustomFieldOptions(String fieldName) {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/fields/" + fieldName + "/options", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch custom field options for {}: {}", fieldName, e.getMessage());
            return List.of();
        }
    }

    public void addFieldOption(String fieldName, String optionValue) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = Map.of("fieldName", fieldName, "value", optionValue);
            restTemplate().postForObject(
                    issueServiceUrl + "/api/fields/" + fieldName + "/options",
                    new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to add field option for {}: {}", fieldName, e.getMessage());
        }
    }

    // === Project Management (Task 2.6) ===

    public void archiveVersion(UUID versionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate().exchange(
                    versionServiceUrl + "/api/versions/" + versionId + "/archive",
                    HttpMethod.PATCH, new HttpEntity<>(Map.of(), headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to archive version {}: {}", versionId, e.getMessage());
        }
    }

    public void deleteVersion(UUID versionId) {
        try {
            restTemplate().delete(versionServiceUrl + "/api/versions/" + versionId);
        } catch (Exception e) {
            log.warn("Failed to delete version {}: {}", versionId, e.getMessage());
        }
    }

    public void unreleaseVersion(UUID versionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate().exchange(
                    versionServiceUrl + "/api/versions/" + versionId + "/unrelease",
                    HttpMethod.PATCH, new HttpEntity<>(Map.of(), headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to unrelease version {}: {}", versionId, e.getMessage());
        }
    }

    public void deleteComponent(UUID componentId) {
        try {
            restTemplate().delete(componentServiceUrl + "/api/components/" + componentId);
        } catch (Exception e) {
            log.warn("Failed to delete component {}: {}", componentId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchProjectRoles(UUID projectId) {
        try {
            List<?> response = restTemplate().getForObject(
                    projectServiceUrl + "/api/projects/" + projectId + "/roles", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch roles for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchProjects(String query) {
        try {
            String url = projectServiceUrl + "/api/projects" + (query != null ? "?search=" + query : "");
            List<?> response = restTemplate().getForObject(url, List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to search projects: {}", e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> fetchProjectProperties(UUID projectId) {
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    projectServiceUrl + "/api/projects/" + projectId + "/properties", Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch project properties: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public void setProjectProperty(UUID projectId, String key, Object value) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("key", key, "value", value);
            restTemplate().put(projectServiceUrl + "/api/projects/" + projectId + "/properties/" + key,
                    new HttpEntity<>(body, headers));
        } catch (Exception e) {
            log.warn("Failed to set project property: {}", e.getMessage());
        }
    }

    // === Workflow Functions (Task 2.7) ===

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchAvailableTransitions(UUID issueId) {
        try {
            List<?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/transitions", List.class);
            if (response == null) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map<?, ?> m) result.add(castMap(m));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch available transitions for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    public Map<String, Object> fetchWorkflowForIssue(UUID issueId) {
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/issues/" + issueId + "/workflow", Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch workflow for issue {}: {}", issueId, e.getMessage());
            return new HashMap<>();
        }
    }

    // === Issue Functions (Task 2.8) ===

    public void unlinkIssues(UUID sourceId, UUID targetId) {
        try {
            restTemplate().delete(
                    issueServiceUrl + "/api/issues/links?sourceIssueId=" + sourceId + "&targetIssueId=" + targetId);
        } catch (Exception e) {
            log.warn("Failed to unlink issues {} and {}: {}", sourceId, targetId, e.getMessage());
        }
    }

    public void setIssueRank(UUID issueId, int rank) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate().put(
                    planServiceUrl + "/api/ranking/" + issueId,
                    new HttpEntity<>(Map.of("rank", rank), headers));
        } catch (Exception e) {
            log.warn("Failed to set rank for issue {}: {}", issueId, e.getMessage());
        }
    }

    // === Worklog Completion (Task 2.9) ===

    public void deleteWorklog(UUID worklogId) {
        try {
            restTemplate().delete(issueServiceUrl + "/api/worklogs/" + worklogId);
        } catch (Exception e) {
            log.warn("Failed to delete worklog {}: {}", worklogId, e.getMessage());
        }
    }

    public void updateWorklog(UUID worklogId, String timeSpent, String comment) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("timeSpent", timeSpent);
            if (comment != null) body.put("comment", comment);
            restTemplate().put(
                    issueServiceUrl + "/api/worklogs/" + worklogId,
                    new HttpEntity<>(body, headers));
        } catch (Exception e) {
            log.warn("Failed to update worklog {}: {}", worklogId, e.getMessage());
        }
    }

    // === User/Group Admin (SIL parity) ===

    public Map<String, Object> createUser(String username, String email, String displayName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("username", username);
            body.put("email", email);
            if (displayName != null) body.put("displayName", displayName);
            Map<?, ?> response = restTemplate().postForObject(
                    userServiceUrl + "/api/admin/users",
                    new HttpEntity<>(body, headers), Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to create user {}: {}", username, e.getMessage());
            return new HashMap<>();
        }
    }

    public void deactivateUser(UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate().exchange(
                    userServiceUrl + "/api/admin/users/" + userId + "/deactivate",
                    HttpMethod.PATCH, new HttpEntity<>(Map.of(), headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to deactivate user {}: {}", userId, e.getMessage());
        }
    }

    public void deleteUser(UUID userId) {
        try {
            restTemplate().delete(userServiceUrl + "/api/admin/users/" + userId);
        } catch (Exception e) {
            log.warn("Failed to delete user {}: {}", userId, e.getMessage());
        }
    }

    public Map<String, Object> createGroup(String groupName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<?, ?> response = restTemplate().postForObject(
                    userServiceUrl + "/api/admin/groups",
                    new HttpEntity<>(Map.of("name", groupName), headers), Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to create group {}: {}", groupName, e.getMessage());
            return new HashMap<>();
        }
    }

    public void deleteGroup(String groupName) {
        try {
            restTemplate().delete(userServiceUrl + "/api/admin/groups/" + groupName);
        } catch (Exception e) {
            log.warn("Failed to delete group {}: {}", groupName, e.getMessage());
        }
    }

    // === Comment Visibility (SIL parity) ===

    public void updateCommentVisibility(UUID commentId, Map<String, Object> restriction) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate().exchange(
                    commentServiceUrl + "/api/comments/" + commentId + "/visibility",
                    HttpMethod.PATCH, new HttpEntity<>(restriction, headers), Map.class);
        } catch (Exception e) {
            log.warn("Failed to update comment visibility {}: {}", commentId, e.getMessage());
        }
    }

    // === Workflow Completion (SIL parity) ===

    public Map<String, Object> fetchWorkflowScheme(UUID projectId) {
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    projectServiceUrl + "/api/projects/" + projectId + "/workflow-scheme", Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch workflow scheme for project {}: {}", projectId, e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> fetchTransitionProperties(UUID transitionId) {
        try {
            Map<?, ?> response = restTemplate().getForObject(
                    issueServiceUrl + "/api/workflows/transitions/" + transitionId + "/properties", Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch transition properties for {}: {}", transitionId, e.getMessage());
            return new HashMap<>();
        }
    }

    // === Security — User Permissions (SIL parity) ===

    @SuppressWarnings("unchecked")
    public List<String> fetchUserPermissions(UUID userId) {
        try {
            List<?> response = restTemplate().getForObject(
                    userServiceUrl + "/api/admin/users/" + userId + "/permissions", List.class);
            if (response == null) return List.of();
            List<String> result = new ArrayList<>();
            for (Object item : response) {
                result.add(String.valueOf(item));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch permissions for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
}
