package com.avionics_systems.migration.service.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class WorkflowSchemeServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "workflowSchemeService";

    @Autowired
    public WorkflowSchemeServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.workflowServiceUrl:http://localhost:8085}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "workflowSchemeService";
    }

    @Override
    protected String getServicePathPrefix() {
        return "/api/workflow-schemes";
    }

    public Map<String, Object> createScheme(String name, String description, UUID defaultWorkflowId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("description", description != null ? description : "Imported from Legacy DC workflow scheme XML");
        body.put("isDefault", false);
        if (defaultWorkflowId != null) {
            body.put("defaultWorkflowId", defaultWorkflowId.toString());
        }
        return executePost("", body, Map.class);
    }

    public Map<String, Object> addMapping(UUID schemeId, UUID issueTypeId, UUID workflowId) {
        Map<String, Object> body = Map.of(
                "issueTypeId", issueTypeId.toString(),
                "workflowId", workflowId.toString()
        );
        return executePost("/" + schemeId + "/mappings", body, Map.class);
    }

    public List<Map<String, Object>> listSchemes() {
        ParameterizedTypeReference<List<Map<String, Object>>> typeRef = new ParameterizedTypeReference<>() {};
        String url = buildUrl("");
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
        try {
            ResponseEntity<List<Map<String, Object>>> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, typeRef);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (RestClientException e) {
            throw ServiceClientException.connectionError(serviceName, getServicePathPrefix(), e);
        }
    }

    public void deleteScheme(String schemeId) {
        executeDelete("/" + schemeId);
    }
}
