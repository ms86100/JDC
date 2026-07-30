package com.avionics_systems.issue.performance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Performance Metrics Controller
 * Phase 7 - Polish & Performance
 * Exposes performance metrics via REST endpoints
 */
@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance", description = "Performance monitoring and benchmarking endpoints")
public class PerformanceController {

    private final PerformanceMonitor performanceMonitor;

    public PerformanceController(PerformanceMonitor performanceMonitor) {
        this.performanceMonitor = performanceMonitor;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get all performance metrics",
               description = "Returns current performance metrics for all endpoints")
    public PerformanceMetricsResponse getMetrics() {
        Map<String, PerformanceMonitor.EndpointMetrics> endpointMetrics = performanceMonitor.getAllMetrics();
        PerformanceMonitor.SystemMetrics systemMetrics = performanceMonitor.getSystemMetrics();

        return new PerformanceMetricsResponse(
                systemMetrics,
                endpointMetrics,
                performanceMonitor.getCurrentThroughput()
        );
    }

    @GetMapping("/metrics/system")
    @Operation(summary = "Get system metrics",
               description = "Returns current system-level metrics (CPU, memory, threads)")
    public PerformanceMonitor.SystemMetrics getSystemMetrics() {
        return performanceMonitor.getSystemMetrics();
    }

    @GetMapping("/metrics/health")
    @Operation(summary = "Get system health status",
               description = "Returns a simple health check based on system metrics")
    public HealthStatus getHealthStatus() {
        PerformanceMonitor.SystemMetrics metrics = performanceMonitor.getSystemMetrics();

        // Simple health check based on memory usage
        double memoryUsagePercent = (double) metrics.usedMemory() / metrics.totalMemory() * 100;
        boolean healthy = memoryUsagePercent < 90 && metrics.systemLoad() < 10;

        String status = healthy ? "HEALTHY" : "DEGRADED";
        String message = healthy ? "All systems operational" :
                String.format("High resource usage - Memory: %.1f%%, Load: %.2f",
                        memoryUsagePercent, metrics.systemLoad());

        return new HealthStatus(status, message, memoryUsagePercent, metrics.systemLoad());
    }

    // Response records
    public record PerformanceMetricsResponse(
            PerformanceMonitor.SystemMetrics system,
            Map<String, PerformanceMonitor.EndpointMetrics> endpoints,
            double throughput
    ) {}

    public record HealthStatus(
            String status,
            String message,
            double memoryUsagePercent,
            double systemLoad
    ) {}
}