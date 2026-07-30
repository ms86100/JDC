package com.avionics_systems.issue.performance;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Performance Monitoring Component
 * Phase 7 - Polish & Performance
 * Provides real-time performance metrics for the service
 */
@Component
public class PerformanceMonitor {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

    // Metrics storage
    private final Map<String, AtomicLong> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> responseTimeAggregators = new ConcurrentHashMap<>();

    // Track min/max/avg response times per endpoint
    private final Map<String, ResponseTimeStats> responseTimeStats = new ConcurrentHashMap<>();

    public void recordRequest(String endpoint) {
        requestCounters.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordError(String endpoint) {
        errorCounters.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordResponseTime(String endpoint, long responseTimeMs) {
        ResponseTimeStats stats = responseTimeStats.computeIfAbsent(endpoint, k -> new ResponseTimeStats());
        stats.record(responseTimeMs);
    }

    /**
     * Get current system metrics
     */
    public SystemMetrics getSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        return new SystemMetrics(
                osMXBean.getSystemLoadAverage(),
                runtime.availableProcessors(),
                usedMemory,
                totalMemory,
                threadMXBean.getThreadCount(),
                threadMXBean.getPeakThreadCount()
        );
    }

    /**
     * Get metrics for a specific endpoint
     */
    public EndpointMetrics getEndpointMetrics(String endpoint) {
        long requests = requestCounters.getOrDefault(endpoint, new AtomicLong(0)).get();
        long errors = errorCounters.getOrDefault(endpoint, new AtomicLong(0)).get();
        ResponseTimeStats stats = responseTimeStats.getOrDefault(endpoint, new ResponseTimeStats());

        return new EndpointMetrics(
                endpoint,
                requests,
                errors,
                requests > 0 ? (errors * 100.0 / requests) : 0.0,
                stats.getMin(),
                stats.getMax(),
                stats.getAvg()
        );
    }

    /**
     * Get all endpoint metrics
     */
    public Map<String, EndpointMetrics> getAllMetrics() {
        Map<String, EndpointMetrics> metrics = new HashMap<>();
        requestCounters.keySet().forEach(endpoint -> metrics.put(endpoint, getEndpointMetrics(endpoint)));
        return metrics;
    }

    /**
     * Get current throughput (requests per second)
     */
    public double getCurrentThroughput() {
        long totalRequests = requestCounters.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
        // Estimate based on startup time (in production, use a time window)
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return uptimeSeconds > 0 ? (double) totalRequests / uptimeSeconds : 0.0;
    }

    /**
     * Reset all metrics (for testing)
     */
    public void resetMetrics() {
        requestCounters.clear();
        errorCounters.clear();
        responseTimeStats.clear();
    }

    // Inner classes for metrics
    public record SystemMetrics(
            double systemLoad,
            int availableProcessors,
            long usedMemory,
            long totalMemory,
            int threadCount,
            int peakThreadCount
    ) {}

    public record EndpointMetrics(
            String endpoint,
            long totalRequests,
            long errorCount,
            double errorRate,
            long minResponseTime,
            long maxResponseTime,
            double avgResponseTime
    ) {}

    /**
     * Thread-safe stats calculator
     */
    private static class ResponseTimeStats {
        private volatile long min = Long.MAX_VALUE;
        private volatile long max = 0;
        private volatile long count = 0;
        private volatile long sum = 0;

        synchronized void record(long value) {
            if (value < min) min = value;
            if (value > max) max = value;
            count++;
            sum += value;
        }

        long getMin() { return min == Long.MAX_VALUE ? 0 : min; }
        long getMax() { return max; }
        double getAvg() { return count > 0 ? (double) sum / count : 0.0; }
    }

    // Uses java.util.concurrent.atomic.LongAdder for thread-safe aggregation
}