package com.jira.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.admin.entity.WorkflowSchemeEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxies admin workflow scheme operations to jira-workflow-service (canonical jira_workflow schema).
 */
@Service
@Slf4j
public class WorkflowSchemeAdminProxyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jira.services.workflow-url:http://localhost:8085}")
    private String workflowServiceUrl;

    public List<WorkflowSchemeEntity> listSchemes() {
        JsonNode body = restTemplate.getForObject(workflowServiceUrl + "/api/workflow-schemes", JsonNode.class);
        if (body == null || !body.isArray()) {
            return List.of();
        }
        List<WorkflowSchemeEntity> result = new ArrayList<>();
        for (JsonNode node : body) {
            result.add(toEntity(node));
        }
        return result;
    }

    public WorkflowSchemeEntity createScheme(Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", data.get("name"));
        payload.put("description", data.getOrDefault("description", ""));
        if (data.get("defaultWorkflowId") != null) {
            payload.put("defaultWorkflowId", data.get("defaultWorkflowId"));
        }
        if (data.get("isDefault") != null) {
            payload.put("isDefault", data.get("isDefault"));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonNode body = restTemplate.postForObject(
                workflowServiceUrl + "/api/workflow-schemes",
                new HttpEntity<>(payload, headers),
                JsonNode.class);
        return toEntity(body);
    }

    public void assignSchemeToProjects(String schemeId, List<String> projectIds) {
        Map<String, Object> body = Map.of("schemeId", schemeId, "projectIds", projectIds);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForObject(
                workflowServiceUrl + "/api/workflow-schemes/projects/assign-bulk",
                new HttpEntity<>(body, headers),
                Map.class);
    }

    private WorkflowSchemeEntity toEntity(JsonNode node) {
        if (node == null) {
            return WorkflowSchemeEntity.builder().name("unknown").build();
        }
        return WorkflowSchemeEntity.builder()
                .id(node.hasNonNull("id") ? node.get("id").asText() : null)
                .name(node.hasNonNull("name") ? node.get("name").asText() : null)
                .description(node.hasNonNull("description") ? node.get("description").asText() : "")
                .defaultWorkflowId(node.hasNonNull("defaultWorkflowId") ? node.get("defaultWorkflowId").asText() : null)
                .isDefault(node.hasNonNull("isDefault") && node.get("isDefault").asBoolean())
                .projectCount(node.hasNonNull("projectCount") ? node.get("projectCount").asInt() : 0)
                .build();
    }
}
