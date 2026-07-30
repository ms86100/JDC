package com.avionics_systems.migration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MigrationObservabilityService {

    private final RestTemplate restTemplate;

    @Value("${server.port:8094}")
    private int serverPort;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorBasePath;

    public Map<String, Object> observabilityLinks() {
        Map<String, Object> body = new LinkedHashMap<>();
        String base = "http://localhost:" + serverPort;
        body.put("service", "avionics-systems-migration-service");
        body.put("healthUrl", base + actuatorBasePath + "/health");
        body.put("metricsUrl", base + actuatorBasePath + "/metrics");
        body.put("prometheusUrl", base + actuatorBasePath + "/prometheus");
        body.put("infoUrl", base + actuatorBasePath + "/info");
        body.put("migrationCenterPath", "/migration");
        body.put("notes", "OpenTelemetry: export via Micrometer OTLP registry when OTEL_EXPORTER_OTLP_ENDPOINT is set");

        Map<String, Object> probes = new LinkedHashMap<>();
        probes.put("health", probe(base + actuatorBasePath + "/health"));
        body.put("probes", probes);
        body.put("checkedAt", java.time.Instant.now().toString());
        return body;
    }

    private Map<String, Object> probe(String url) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("url", url);
        try {
            var response = restTemplate.getForEntity(url, Map.class);
            row.put("status", response.getStatusCode().is2xxSuccessful() ? "UP" : "DOWN");
            row.put("httpStatus", response.getStatusCode().value());
        } catch (Exception e) {
            row.put("status", "DOWN");
            row.put("error", e.getMessage());
        }
        return row;
    }
}
