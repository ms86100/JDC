package com.jira.issue.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class ProjectServiceClient {

    private final RestTemplate restTemplate;
    private final String projectServiceUrl;

    public ProjectServiceClient(RestTemplate restTemplate,
                                @Value("${project.service.url}") String projectServiceUrl) {
        this.restTemplate = restTemplate;
        this.projectServiceUrl = projectServiceUrl;
    }

    @SuppressWarnings("unchecked")
    public String getProjectKey(UUID projectId) {
        try {
            String url = String.format("%s/api/projects/%s", projectServiceUrl, projectId);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("projectKey") != null) {
                return response.get("projectKey").toString();
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to get project key for {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProject(UUID projectId) {
        try {
            String url = String.format("%s/api/projects/%s", projectServiceUrl, projectId);
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("Failed to get project {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getIssueTypeScheme(UUID projectId) {
        try {
            Map<String, Object> project = getProject(projectId);
            if (project != null && project.get("issueTypeSchemeId") != null) {
                String schemeUrl = String.format("%s/api/projects/schemes/issue-type-schemes/%s/issue-types",
                        projectServiceUrl, project.get("issueTypeSchemeId"));
                return restTemplate.getForObject(schemeUrl, List.class);
            }
        } catch (Exception e) {
            log.debug("Could not fetch issue type scheme for project {}: {}", projectId, e.getMessage());
        }
        return List.of();
    }

    public boolean isIssueTypeValidInProject(UUID projectId, UUID issueTypeId) {
        List<Map<String, Object>> types = getIssueTypeScheme(projectId);
        if (types.isEmpty()) return true;
        return types.stream()
                .anyMatch(t -> issueTypeId.toString().equals(String.valueOf(t.get("id"))));
    }
}
