package com.jira.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Health Check Controller for comprehensive system health monitoring.
 * Provides detailed health status for all platform services.
 * Phase 7 - Polish & Performance
 */
@RestController
@RequestMapping("/api/admin/health")
@Slf4j
@Tag(name = "Health Check", description = "System health and monitoring endpoints")
@CrossOrigin(origins = "*")
public class HealthCheckController {

    private final DataSource dataSource;

    @Value("${spring.application.name:jira-admin-service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    public HealthCheckController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Basic health check endpoint.
     */
    @GetMapping
    @Operation(summary = "Basic health check", description = "Returns basic health status")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("service", applicationName);
        health.put("port", serverPort);
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with all service dependencies.
     */
    @GetMapping("/detailed")
    @Operation(summary = "Detailed health check", description = "Returns detailed health status of all dependencies")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("service", applicationName);

        // Check database connectivity
        Map<String, Object> databaseHealth = checkDatabaseHealth();
        health.put("database", databaseHealth);

        // System metrics
        Map<String, Object> systemHealth = checkSystemHealth();
        health.put("system", systemHealth);

        // Application info
        Map<String, Object> appInfo = checkApplicationInfo();
        health.put("application", appInfo);

        // Determine overall status
        String overallStatus = determineOverallStatus(health);
        health.put("status", overallStatus);

        HttpStatus httpStatus = "UP".equals(overallStatus) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }

    /**
     * Readiness probe - is the service ready to receive traffic?
     */
    @GetMapping("/ready")
    @Operation(summary = "Readiness probe", description = "Checks if service is ready to receive traffic")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readiness = new HashMap<>();

        // Check database connectivity
        boolean dbReady = isDatabaseReady();
        readiness.put("database_ready", dbReady);
        readiness.put("ready", dbReady);

        if (dbReady) {
            return ResponseEntity.ok(readiness);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(readiness);
        }
    }

    /**
     * Liveness probe - is the service alive?
     */
    @GetMapping("/live")
    @Operation(summary = "Liveness probe", description = "Checks if service is alive")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("alive", true);
        liveness.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(liveness);
    }

    /**
     * Check database connectivity and performance.
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            // Check basic connectivity
            Instant start = Instant.now();
            boolean isValid = conn.isValid(5);
            long responseTime = Duration.between(start, Instant.now()).toMillis();

            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("response_time_ms", responseTime);

            // Get connection pool info if available
            try {
                // Try HikariCP (Spring Boot default)
                if (dataSource.getClass().getName().contains("Hikari")) {
                    Map<String, Object> poolInfo = new HashMap<>();
                    poolInfo.put("type", "HikariCP");
                    dbHealth.put("connection_pool", poolInfo);
                }
            } catch (Exception e) {
                log.debug("Could not get connection pool info: {}", e.getMessage());
            }

            // Test query execution
            start = Instant.now();
            conn.prepareStatement("SELECT 1").execute();
            dbHealth.put("query_time_ms", Duration.between(start, Instant.now()).toMillis());

        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }

        return dbHealth;
    }

    /**
     * Check system resource health.
     */
    private Map<String, Object> checkSystemHealth() {
        Map<String, Object> systemHealth = new HashMap<>();

        // Runtime info
        Runtime runtime = Runtime.getRuntime();
        systemHealth.put("free_memory_mb", runtime.freeMemory() / 1024 / 1024);
        systemHealth.put("total_memory_mb", runtime.totalMemory() / 1024 / 1024);
        systemHealth.put("max_memory_mb", runtime.maxMemory() / 1024 / 1024);
        systemHealth.put("available_processors", runtime.availableProcessors());

        // Memory usage
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (usedMemory * 100.0) / totalMemory;

        systemHealth.put("memory_usage_percent", Math.round(memoryUsagePercent * 100.0) / 100.0);
        systemHealth.put("memory_status", memoryUsagePercent > 90 ? "CRITICAL" :
                memoryUsagePercent > 75 ? "WARNING" : "OK");

        // Uptime
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        systemHealth.put("uptime_ms", uptimeMs);
        systemHealth.put("uptime_formatted", formatUptime(uptimeMs));

        return systemHealth;
    }

    /**
     * Get application information.
     */
    private Map<String, Object> checkApplicationInfo() {
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("name", applicationName);
        appInfo.put("port", serverPort);
        appInfo.put("version", "1.0.0");
        appInfo.put("java_version", System.getProperty("java.version"));
        appInfo.put("os_name", System.getProperty("os.name"));
        return appInfo;
    }

    /**
     * Determine overall system status based on all health checks.
     */
    private String determineOverallStatus(Map<String, Object> health) {
        // Check database
        @SuppressWarnings("unchecked")
        Map<String, Object> dbHealth = (Map<String, Object>) health.get("database");
        if (dbHealth != null && !"UP".equals(dbHealth.get("status"))) {
            return "DOWN";
        }

        // Check system
        @SuppressWarnings("unchecked")
        Map<String, Object> systemHealth = (Map<String, Object>) health.get("system");
        if (systemHealth != null) {
            String memoryStatus = (String) systemHealth.get("memory_status");
            if ("CRITICAL".equals(memoryStatus)) {
                return "DEGRADED";
            }
        }

        return "UP";
    }

    /**
     * Check if database is ready.
     */
    private boolean isDatabaseReady() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.error("Database not ready: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Format uptime in human-readable format.
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}