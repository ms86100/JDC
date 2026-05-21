package com.jira.migration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationServicesHealthService {

    private final RestTemplate restTemplate;

    @Value("${services.projectServiceUrl:http://localhost:8081}")
    private String projectServiceUrl;

    @Value("${services.issueServiceUrl:http://localhost:8082}")
    private String issueServiceUrl;

    @Value("${services.workflowServiceUrl:http://localhost:8085}")
    private String workflowServiceUrl;

    @Value("${services.fieldServiceUrl:http://localhost:8086}")
    private String fieldServiceUrl;

    @Value("${services.userServiceUrl:http://localhost:8083}")
    private String userServiceUrl;

    @Value("${services.adminServiceUrl:http://localhost:8093}")
    private String adminServiceUrl;

    public Map<String, Object> checkAll() {
        List<Map<String, Object>> services = List.of(
                probe("project-service", projectServiceUrl, "/actuator/health"),
                probe("issue-service", issueServiceUrl, "/actuator/health"),
                probe("workflow-service", workflowServiceUrl, "/actuator/health"),
                probe("field-service", fieldServiceUrl, "/actuator/health"),
                probe("user-service", userServiceUrl, "/actuator/health"),
                probe("admin-service", adminServiceUrl, "/actuator/health")
        );
        long up = services.stream().filter(s -> "UP".equals(s.get("status"))).count();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("overallStatus", up == services.size() ? "UP" : up > 0 ? "DEGRADED" : "DOWN");
        body.put("servicesUp", up);
        body.put("servicesTotal", services.size());
        body.put("services", services);
        body.put("checkedAt", java.time.Instant.now().toString());
        return body;
    }

    private Map<String, Object> probe(String name, String baseUrl, String path) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("baseUrl", baseUrl);
        String url = baseUrl.replaceAll("/$", "") + path;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            String status = response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN";
            row.put("status", status);
            row.put("httpStatus", response.getStatusCode().value());
            if (response.getBody() != null) {
                Object healthStatus = response.getBody().get("status");
                if (healthStatus != null) {
                    row.put("actuatorStatus", healthStatus.toString());
                }
            }
        } catch (Exception e) {
            row.put("status", "DOWN");
            row.put("error", e.getMessage());
            log.debug("{} health probe failed: {}", name, e.getMessage());
        }
        return row;
    }
}
