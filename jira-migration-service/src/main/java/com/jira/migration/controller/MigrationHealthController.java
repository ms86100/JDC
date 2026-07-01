package com.jira.migration.controller;

import com.jira.migration.cluster.ClusterHealth;
import com.jira.migration.cluster.ClusterHealthMonitor;
import com.jira.migration.service.MigrationObservabilityService;
import com.jira.migration.service.MigrationServicesHealthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/migration/health")
@RequiredArgsConstructor
@Tag(name = "Migration Health", description = "Downstream service health for Migration Center")
public class MigrationHealthController {

    private final MigrationServicesHealthService servicesHealthService;
    private final ClusterHealthMonitor clusterHealthMonitor;
    private final MigrationObservabilityService observabilityService;

    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> services() {
        return ResponseEntity.ok(servicesHealthService.checkAll());
    }

    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> cluster() {
        ClusterHealth health = clusterHealthMonitor.getClusterHealth();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", health.getStatus() != null ? health.getStatus().name() : "UNKNOWN");
        body.put("activeNodes", health.getActiveNodes());
        body.put("totalNodes", health.getTotalNodes());
        body.put("availabilityPercentage", health.getAvailabilityPercentage());
        body.put("unhealthyNodes", health.getUnhealthyNodes());
        body.put("warnings", health.getWarnings());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/observability")
    public ResponseEntity<Map<String, Object>> observability() {
        return ResponseEntity.ok(observabilityService.observabilityLinks());
    }
}
