package com.jira.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class MigrationServicesHealthService {

    private final RestTemplate restTemplate;
    private final List<ServiceProbe> platformServices;

    public MigrationServicesHealthService(
            RestTemplate restTemplate,
            @Value("${services.gatewayServiceUrl:http://localhost:8080}") String gatewayUrl,
            @Value("${services.authServiceUrl:http://localhost:8081}") String authUrl,
            @Value("${services.userServiceUrl:http://localhost:8082}") String userUrl,
            @Value("${services.projectServiceUrl:http://localhost:8083}") String projectUrl,
            @Value("${services.issueServiceUrl:http://localhost:8084}") String issueUrl,
            @Value("${services.workflowServiceUrl:http://localhost:8085}") String workflowUrl,
            @Value("${services.commentServiceUrl:http://localhost:8086}") String commentUrl,
            @Value("${services.notificationServiceUrl:http://localhost:8087}") String notificationUrl,
            @Value("${services.searchServiceUrl:http://localhost:8088}") String searchUrl,
            @Value("${services.auditServiceUrl:http://localhost:8089}") String auditUrl,
            @Value("${services.attachmentServiceUrl:http://localhost:8090}") String attachmentUrl,
            @Value("${services.sprintServiceUrl:http://localhost:8091}") String sprintUrl,
            @Value("${services.planServiceUrl:http://localhost:8092}") String planUrl,
            @Value("${services.adminServiceUrl:http://localhost:8093}") String adminUrl,
            @Value("${services.testServiceUrl:http://localhost:8095}") String testUrl) {
        this.restTemplate = restTemplate;
        this.platformServices = List.of(
                new ServiceProbe("gateway", gatewayUrl),
                new ServiceProbe("auth", authUrl),
                new ServiceProbe("user", userUrl),
                new ServiceProbe("project", projectUrl),
                new ServiceProbe("issue", issueUrl),
                new ServiceProbe("workflow", workflowUrl),
                new ServiceProbe("comment", commentUrl),
                new ServiceProbe("notification", notificationUrl),
                new ServiceProbe("search", searchUrl),
                new ServiceProbe("audit", auditUrl),
                new ServiceProbe("attachment", attachmentUrl),
                new ServiceProbe("sprint", sprintUrl),
                new ServiceProbe("plan", planUrl),
                new ServiceProbe("admin", adminUrl),
                new ServiceProbe("migration", "http://localhost:8094"),
                new ServiceProbe("test", testUrl)
        );
    }

    public Map<String, Object> checkAll() {
        List<Map<String, Object>> services = platformServices.stream()
                .map(p -> probe(p.name() + "-service", p.baseUrl(), "/actuator/health"))
                .toList();
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
            var response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, null, Map.class);
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

    private record ServiceProbe(String name, String baseUrl) {}
}
