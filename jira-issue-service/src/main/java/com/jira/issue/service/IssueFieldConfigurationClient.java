package com.jira.issue.service;

import com.jira.issue.dto.CreateIssueRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class IssueFieldConfigurationClient {

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public List<String> validateCreate(UUID projectId, CreateIssueRequest request) {
        if (projectId == null || request == null) {
            return List.of();
        }
        try {
            String url = projectServiceUrl + "/api/projects/" + projectId + "/field-configuration/validate-create";
            Map<String, Object> body = new HashMap<>();
            body.put("issueTypeId", request.getIssueTypeId());
            body.put("fields", toFieldMap(request));

            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);
            if (response == null) {
                return List.of();
            }
            Object errors = response.get("errors");
            if (errors instanceof List<?> list) {
                return list.stream().map(Object::toString).toList();
            }
            return List.of();
        } catch (Exception e) {
            log.warn("Field configuration validation skipped for project {}: {}", projectId, e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> toFieldMap(CreateIssueRequest request) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("title", request.getTitle());
        fields.put("summary", request.getTitle());
        fields.put("description", request.getDescription());
        fields.put("issueTypeId", request.getIssueTypeId());
        fields.put("priorityId", request.getPriorityId());
        fields.put("assigneeId", request.getAssigneeId());
        fields.put("dueDate", request.getDueDate());
        fields.put("labels", request.getLabels());
        fields.put("componentIds", request.getComponentIds());
        fields.put("fixVersions", request.getFixVersions());
        fields.put("affectsVersions", request.getAffectsVersions());
        fields.put("storyPoints", request.getStoryPoints());
        fields.put("parentIssueId", request.getParentIssueId());
        return fields;
    }
}
