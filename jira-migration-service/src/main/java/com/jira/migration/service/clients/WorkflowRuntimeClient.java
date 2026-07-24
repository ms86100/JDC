package com.jira.migration.service.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowRuntimeClient {

    private final RestTemplate restTemplate;

    @Value("${jira.services.workflow-url:http://localhost:8085}")
    private String workflowServiceUrl;

    @Value("${jira.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAvailableTransitions(UUID issueId, UUID projectId, UUID userId) {
        try {
            String url = workflowServiceUrl + "/api/workflows/issues/" + issueId
                    + "/available-transitions?projectId=" + projectId;
            HttpHeaders headers = new HttpHeaders();
            if (userId != null) {
                headers.set("X-User-Id", userId.toString());
            }
            Map<?, ?> body = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
            if (body == null) {
                return List.of();
            }
            Object transitions = body.get("transitions");
            if (transitions instanceof List<?> list) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        Map<String, Object> row = new HashMap<>();
                        m.forEach((k, v) -> row.put(String.valueOf(k), v));
                        result.add(row);
                    }
                }
                return result;
            }
            return List.of();
        } catch (Exception e) {
            log.warn("Failed to load transitions for issue {}: {}", issueId, e.getMessage());
            return List.of();
        }
    }

    public UUID findTransitionIdToStatus(UUID issueId, UUID projectId, UUID targetStatusId, UUID userId) {
        for (Map<String, Object> t : getAvailableTransitions(issueId, projectId, userId)) {
            Object to = t.get("toStatusId");
            if (to != null && targetStatusId.toString().equals(to.toString())) {
                Object id = t.get("id");
                if (id != null) {
                    return UUID.fromString(id.toString());
                }
            }
        }
        return null;
    }

    public void executeTransition(UUID issueId, UUID projectId, UUID transitionId, UUID userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("issueId", issueId.toString());
        body.put("projectId", projectId.toString());
        body.put("transitionId", transitionId.toString());
        if (userId != null) {
            body.put("userId", userId.toString());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (userId != null) {
            headers.set("X-User-Id", userId.toString());
        }
        restTemplate.postForObject(
                workflowServiceUrl + "/api/workflows/transitions/execute",
                new HttpEntity<>(body, headers),
                Map.class);
    }

    @SuppressWarnings("unchecked")
    public UUID resolveStatusIdByName(String statusName) {
        if (statusName == null || statusName.isBlank()) {
            return null;
        }
        try {
            List<Map<String, Object>> statuses = restTemplate.exchange(
                    issueServiceUrl + "/api/issues/statuses",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
            if (statuses == null) {
                return null;
            }
            for (Map<String, Object> s : statuses) {
                Object name = s.get("name");
                if (name != null && statusName.equalsIgnoreCase(name.toString())) {
                    Object id = s.get("id");
                    if (id != null) {
                        return UUID.fromString(id.toString());
                    }
                }
            }
            try {
                return UUID.fromString(statusName.trim());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        } catch (Exception e) {
            log.warn("Status lookup failed for '{}': {}", statusName, e.getMessage());
            return null;
        }
    }

    public void applyStatusInternal(UUID issueId, UUID projectId, UUID statusId) {
        Map<String, Object> body = new HashMap<>();
        body.put("statusId", statusId.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Workflow-Internal", "true");
        String url = issueServiceUrl + "/api/issues/" + issueId + "/workflow/internal";
        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
    }
}
