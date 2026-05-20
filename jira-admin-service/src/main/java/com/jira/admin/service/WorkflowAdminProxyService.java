package com.jira.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.admin.entity.WorkflowEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Proxies admin workflow CRUD to jira-workflow-service (canonical jira_workflow schema).
 * Local {@link WorkflowEntity} table is no longer written.
 */
@Service
@Slf4j
public class WorkflowAdminProxyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jira.services.workflow-url:http://localhost:8085}")
    private String workflowServiceUrl;

    public List<WorkflowEntity> listWorkflows() {
        JsonNode body = restTemplate.getForObject(workflowServiceUrl + "/api/admin/workflows", JsonNode.class);
        if (body == null || !body.isArray()) {
            return List.of();
        }
        List<WorkflowEntity> result = new ArrayList<>();
        for (JsonNode node : body) {
            result.add(toEntity(node));
        }
        return result;
    }

    public Optional<WorkflowEntity> getWorkflow(String workflowId) {
        try {
            JsonNode body = restTemplate.getForObject(
                    workflowServiceUrl + "/api/admin/workflows/" + workflowId, JsonNode.class);
            return body != null ? Optional.of(toEntity(body)) : Optional.empty();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public WorkflowEntity createWorkflow(Map<String, Object> data) {
        JsonNode body = exchange(HttpMethod.POST, "/api/admin/workflows", data);
        return toEntity(body);
    }

    public WorkflowEntity updateWorkflow(String workflowId, Map<String, Object> updates) {
        JsonNode body = exchange(HttpMethod.PUT, "/api/admin/workflows/" + workflowId, updates);
        return toEntity(body);
    }

    public WorkflowEntity publishWorkflow(String workflowId) {
        JsonNode body = exchange(HttpMethod.POST, "/api/admin/workflows/" + workflowId + "/publish", Map.of());
        return toEntity(body);
    }

    public WorkflowEntity createDraftFromWorkflow(String workflowId) {
        JsonNode body = exchange(HttpMethod.POST, "/api/admin/workflows/" + workflowId + "/draft", Map.of());
        return toEntity(body);
    }

    private JsonNode exchange(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(workflowServiceUrl + path, method, entity, JsonNode.class).getBody();
    }

    private WorkflowEntity toEntity(JsonNode node) {
        Map<String, Object> map = objectMapper.convertValue(node, new TypeReference<>() {});
        return WorkflowEntity.builder()
                .id(stringVal(map.get("id")))
                .name(stringVal(map.get("name")))
                .description(stringVal(map.get("description")))
                .workflowContent("{}")
                .isSystem(boolVal(map.get("isSystem"), false))
                .isActive(boolVal(map.get("isActive"), false))
                .isDraft(boolVal(map.get("isDraft"), true))
                .version(intVal(map.get("version"), 1))
                .build();
    }

    private static String stringVal(Object v) {
        return v != null ? v.toString() : null;
    }

    private static boolean boolVal(Object v, boolean fallback) {
        if (v instanceof Boolean b) return b;
        if (v != null) return Boolean.parseBoolean(v.toString());
        return fallback;
    }

    private static int intVal(Object v, int fallback) {
        if (v instanceof Number n) return n.intValue();
        if (v != null) {
            try {
                return Integer.parseInt(v.toString());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
