package com.jira.issue.service;

import com.jira.issue.dto.IssueResponse;
import com.jira.issue.entity.IssueStatus;
import com.jira.issue.repository.IssueStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueAvailableTransitionsService {

    private final IssueService issueService;
    private final IssueStatusRepository issueStatusRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${workflow.service.url}")
    private String workflowServiceUrl;

    @Value("${jira.workflow.transition-fallback:false}")
    private boolean transitionFallbackEnabled;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAvailableTransitions(UUID issueId, UUID projectId, UUID userId) {
        try {
            String url = workflowServiceUrl + "/api/workflows/issues/" + issueId
                    + "/available-transitions?projectId=" + projectId;
            HttpHeaders headers = new HttpHeaders();
            if (userId != null) {
                headers.set("X-User-Id", userId.toString());
            }
            Map<String, Object> body = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
            if (body != null) {
                return body;
            }
        } catch (Exception e) {
            log.warn("Workflow transitions unavailable for issue {}: {}", issueId, e.getMessage());
        }
        if (transitionFallbackEnabled) {
            return buildIssueServiceFallback(issueId, projectId);
        }
        return buildEmptyTransitions(issueId, projectId);
    }

    private Map<String, Object> buildEmptyTransitions(UUID issueId, UUID projectId) {
        IssueResponse issue = issueService.getIssue(issueId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("issueId", issueId.toString());
        response.put("projectId", projectId != null ? projectId.toString() : issue.getProjectId().toString());
        response.put("currentStatusId", issue.getStatusId() != null ? issue.getStatusId().toString() : null);
        response.put("transitions", List.of());
        return response;
    }

    private Map<String, Object> buildIssueServiceFallback(UUID issueId, UUID projectId) {
        IssueResponse issue = issueService.getIssue(issueId);
        UUID currentStatusId = issue.getStatusId();
        List<Map<String, Object>> items = new ArrayList<>();

        for (IssueStatus status : issueStatusRepository.findCatalogStatuses()) {
            if (status.getId().equals(currentStatusId)) {
                continue;
            }
            UUID syntheticId = UUID.nameUUIDFromBytes(
                    ("issue-fallback:" + issueId + ":" + status.getId()).getBytes(StandardCharsets.UTF_8));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", syntheticId.toString());
            item.put("name", "Move to " + status.getName());
            item.put("toStatusId", status.getId().toString());
            item.put("toStatusName", status.getName());
            item.put("hasScreen", false);
            items.add(item);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("issueId", issueId.toString());
        response.put("projectId", projectId != null ? projectId.toString() : issue.getProjectId().toString());
        response.put("currentStatusId", currentStatusId != null ? currentStatusId.toString() : null);
        response.put("transitions", items);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castTransitionList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Map<String, Object> map = new LinkedHashMap<>();
                m.forEach((k, v) -> map.put(String.valueOf(k), v));
                result.add(map);
            }
        }
        return result;
    }
}
