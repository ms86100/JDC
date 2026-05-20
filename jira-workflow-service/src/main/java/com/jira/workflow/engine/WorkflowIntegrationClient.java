package com.jira.workflow.engine;

import com.jira.workflow.dto.ExecuteTransitionRequest;
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
public class WorkflowIntegrationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${jira.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    @Value("${jira.services.user-url:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${jira.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Value("${jira.services.search-url:http://localhost:8088}")
    private String searchServiceUrl;

    public Map<String, Object> fetchIssue(UUID issueId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(issueServiceUrl + "/api/issues/" + issueId, Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to fetch issue {}: {}", issueId, e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> fetchUser(UUID userId) {
        if (userId == null) {
            return new HashMap<>();
        }
        try {
            Map<?, ?> response = restTemplate.getForObject(userServiceUrl + "/api/users/" + userId, Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch user {}: {}", userId, e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchLinkedIssuesForWorkflow(UUID issueId) {
        try {
            List<?> response = restTemplate.getForObject(
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
            Map<?, ?> response = restTemplate.getForObject(projectServiceUrl + "/api/projects/" + projectId, Map.class);
            return response != null ? castMap(response) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to fetch project {}: {}", projectId, e.getMessage());
            return new HashMap<>();
        }
    }

    public void updateIssueWorkflowInternal(UUID issueId, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Workflow-Internal", "true");
        String url = issueServiceUrl + "/api/issues/" + issueId + "/workflow/internal";
        restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(body, headers), Map.class);
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
            restTemplate.postForObject(
                    issueServiceUrl + "/api/comments",
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
            restTemplate.postForObject(
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
            restTemplate.postForObject(
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
            restTemplate.postForObject(
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
            restTemplate.postForObject(
                    issueServiceUrl + "/api/issues",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Create subtask post-function failed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> result = new HashMap<>();
        raw.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }
}
