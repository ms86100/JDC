package com.avionics_systems.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Value;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance benchmarking endpoints for testing and monitoring.
 * Phase 7 - Polish & Performance
 */
@RestController
@RequestMapping("/api/benchmark")
@Slf4j
@Tag(name = "Benchmark", description = "Performance testing and monitoring endpoints")
public class BenchmarkController {

    private final RouteLocator routeLocator;
    private final String serviceName;

    // Request counters for benchmarking
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final Instant startTime = Instant.now();

    public BenchmarkController(RouteLocator routeLocator,
                               @Value("${app.gateway.service-name:avionics-systems-gateway}") String serviceName) {
        this.routeLocator = routeLocator;
        this.serviceName = serviceName;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Basic health check for the gateway")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("uptime", Duration.between(startTime, Instant.now()).toMillis());
        health.put("service", serviceName);
        return ResponseEntity.ok(health);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get metrics", description = "Get current service metrics including request counts")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // System metrics
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        metrics.put("timestamp", Instant.now().toString());
        metrics.put("uptime_ms", runtimeBean.getUptime());

        // Request metrics
        metrics.put("total_requests", totalRequests.get());
        metrics.put("total_errors", totalErrors.get());
        metrics.put("error_rate", calculateErrorRate());

        // Memory metrics
        Map<String, Object> memory = new HashMap<>();
        memory.put("heap_used_mb", heapUsage.getUsed() / 1024 / 1024);
        memory.put("heap_max_mb", heapUsage.getMax() / 1024 / 1024);
        memory.put("heap_committed_mb", heapUsage.getCommitted() / 1024 / 1024);
        memory.put("non_heap_used_mb", nonHeapUsage.getUsed() / 1024 / 1024);
        metrics.put("memory", memory);

        // Thread metrics
        metrics.put("available_processors", ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
        metrics.put("loaded_classes", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/throughput-test")
    @Operation(summary = "Throughput test", description = "Simple throughput testing endpoint")
    public ResponseEntity<Map<String, Object>> throughputTest() {
        Instant start = Instant.now();

        // Simulate some processing
        int iterations = 1000;
        long sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += i * i;
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("iterations", iterations);
        result.put("elapsed_ms", elapsed);
        result.put("operations_per_second", (iterations * 1000.0) / elapsed);
        result.put("result_sum", sum);

        totalRequests.incrementAndGet();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/latency-test")
    @Operation(summary = "Latency test", description = "Test endpoint latency")
    public ResponseEntity<Map<String, Object>> latencyTest() {
        Instant start = Instant.now();

        // Simulate variable latency
        try {
            Thread.sleep(10 + (long) (Math.random() * 20));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long latencyMs = Duration.between(start, Instant.now()).toMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("latency_ms", latencyMs);
        result.put("timestamp", Instant.now().toString());

        totalRequests.incrementAndGet();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/routes")
    @Operation(summary = "List routes", description = "Get all configured gateway routes")
    public ResponseEntity<Flux<Route>> getRoutes() {
        return ResponseEntity.ok(routeLocator.getRoutes());
    }

    @GetMapping("/benchmark/record-request")
    @Operation(summary = "Record request", description = "Record a successful request for metrics")
    public ResponseEntity<Map<String, Object>> recordRequest() {
        totalRequests.incrementAndGet();
        Map<String, Object> result = new HashMap<>();
        result.put("total_requests", totalRequests.get());
        result.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/benchmark/record-error")
    @Operation(summary = "Record error", description = "Record an error for metrics")
    public ResponseEntity<Map<String, Object>> recordError() {
        totalErrors.incrementAndGet();
        totalRequests.incrementAndGet();
        Map<String, Object> result = new HashMap<>();
        result.put("total_errors", totalErrors.get());
        result.put("total_requests", totalRequests.get());
        result.put("error_rate", calculateErrorRate());
        result.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    private double calculateErrorRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (totalErrors.get() * 100.0) / total;
    }
}