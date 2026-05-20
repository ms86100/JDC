package com.jira.workflow.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class WorkflowOutboxIntegrationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${jira.services.notification-url:http://localhost:8087}")
    private String notificationServiceUrl;

    @Value("${jira.services.search-url:http://localhost:8088}")
    private String searchServiceUrl;

    @Value("${jira.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    public void sendNotification(UUID userId, String type, String title, String message,
                                 String referenceType, UUID referenceId) {
        if (userId == null) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId.toString());
            body.put("type", type);
            body.put("title", title);
            body.put("message", message);
            body.put("referenceType", referenceType);
            body.put("referenceId", referenceId.toString());
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/notifications",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.warn("Failed to send notification to user {}: {}", userId, e.getMessage());
            throw e;
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
            log.warn("Failed to index issue {}: {}", issueId, e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchIssue(UUID issueId) {
        try {
            Map<?, ?> response = restTemplate.getForObject(issueServiceUrl + "/api/issues/" + issueId, Map.class);
            if (response == null) {
                return new HashMap<>();
            }
            Map<String, Object> result = new HashMap<>();
            response.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch issue {} for outbox enrichment: {}", issueId, e.getMessage());
            return new HashMap<>();
        }
    }
}
