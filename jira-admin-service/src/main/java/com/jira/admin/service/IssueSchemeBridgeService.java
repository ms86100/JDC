package com.jira.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jira.admin.entity.IssueTypeSchemeEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pushes admin issue type scheme configuration to jira-project-service for runtime project schemes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueSchemeBridgeService {

    private final RestTemplate restTemplate;

    @Value("${jira.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Value("${jira.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    public void pushSchemeToProjectService(IssueTypeSchemeEntity scheme, List<String> projectIds) {
        List<String> issueTypeKeys = resolveIssueTypeKeys(scheme.getIssueTypeIds());
        String defaultKey = resolveDefaultKey(scheme.getDefaultIssueType(), issueTypeKeys);

        Map<String, Object> body = new HashMap<>();
        body.put("schemeName", scheme.getName());
        body.put("description", scheme.getDescription());
        body.put("issueTypeKeys", issueTypeKeys);
        body.put("defaultIssueTypeKey", defaultKey);
        body.put("projectIds", projectIds);

        String url = projectServiceUrl + "/api/projects/schemes/issue-type/assign";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), JsonNode.class);
            log.info("Pushed issue type scheme '{}' to {} project(s) in project-service", scheme.getName(), projectIds.size());
        } catch (Exception e) {
            log.warn("Project-service scheme bridge failed (admin assignments still saved): {}", e.getMessage());
        }
    }

    private List<String> resolveIssueTypeKeys(String issueTypeIdsCsv) {
        if (issueTypeIdsCsv == null || issueTypeIdsCsv.isBlank()) {
            return List.of();
        }
        Map<String, String> idToKey = fetchIssueTypeIdToKey();
        List<String> keys = new ArrayList<>();
        for (String id : issueTypeIdsCsv.split(",")) {
            String trimmed = id.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            keys.add(idToKey.getOrDefault(trimmed, trimmed));
        }
        return keys;
    }

    private String resolveDefaultKey(String defaultIssueTypeId, List<String> keys) {
        if (defaultIssueTypeId == null || defaultIssueTypeId.isBlank()) {
            return keys.isEmpty() ? null : keys.get(0);
        }
        Map<String, String> idToKey = fetchIssueTypeIdToKey();
        return idToKey.getOrDefault(defaultIssueTypeId, defaultIssueTypeId);
    }

    private Map<String, String> fetchIssueTypeIdToKey() {
        Map<String, String> map = new HashMap<>();
        String url = issueServiceUrl + "/api/admin/issues/issue-types";
        try {
            JsonNode[] types = restTemplate.getForObject(url, JsonNode[].class);
            if (types == null) {
                return map;
            }
            for (JsonNode t : types) {
                if (t.hasNonNull("id")) {
                    String key = t.hasNonNull("issueTypeKey")
                            ? t.get("issueTypeKey").asText()
                            : (t.hasNonNull("name") ? t.get("name").asText().toLowerCase().replace(' ', '_') : t.get("id").asText());
                    map.put(t.get("id").asText(), key);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load issue types from issue-service: {}", e.getMessage());
        }
        return map;
    }
}
