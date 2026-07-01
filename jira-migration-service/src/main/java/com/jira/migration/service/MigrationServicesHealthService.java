package com.jira.migration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Probes downstream platform microservices used by Migration Center imports.
 * URLs align with launcher.py / jira-gateway application-local.yml ports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationServicesHealthService {

    private final RestTemplate restTemplate;

    private static final List<ServiceProbe> PLATFORM_SERVICES = List.of(
            new ServiceProbe("gateway", "http://localhost:8080"),
            new ServiceProbe("auth", "http://localhost:8081"),
            new ServiceProbe("user", "http://localhost:8082"),
            new ServiceProbe("project", "http://localhost:8083"),
            new ServiceProbe("issue", "http://localhost:8084"),
            new ServiceProbe("workflow", "http://localhost:8085"),
            new ServiceProbe("comment", "http://localhost:8086"),
            new ServiceProbe("notification", "http://localhost:8087"),
            new ServiceProbe("search", "http://localhost:8088"),
            new ServiceProbe("audit", "http://localhost:8089"),
            new ServiceProbe("attachment", "http://localhost:8090"),
            new ServiceProbe("sprint", "http://localhost:8091"),
            new ServiceProbe("plan", "http://localhost:8092"),
            new ServiceProbe("admin", "http://localhost:8093"),
            new ServiceProbe("migration", "http://localhost:8094"),
            new ServiceProbe("test", "http://localhost:8095")
    );

    public Map<String, Object> checkAll() {
        List<Map<String, Object>> services = PLATFORM_SERVICES.stream()
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
