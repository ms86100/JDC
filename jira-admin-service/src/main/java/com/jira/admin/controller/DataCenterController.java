package com.jira.admin.controller;

import com.jira.admin.entity.*;
import com.jira.admin.service.DataCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Data Center Administration Controller - Cluster, cache, indexing, services
 */
@RestController
@RequestMapping("/api/admin/datacenter")
@RequiredArgsConstructor
@Tag(name = "Data Center Administration", description = "Data Center Management API")
@CrossOrigin(origins = "*")
public class DataCenterController {

    private final DataCenterService dataCenterService;

    // ==================== Cluster Nodes ====================

    @GetMapping("/cluster/nodes")
    @Operation(summary = "Get all cluster nodes")
    public ResponseEntity<List<ClusterNodeEntity>> getClusterNodes() {
        return ResponseEntity.ok(dataCenterService.getClusterNodes());
    }

    @GetMapping("/cluster/nodes/{nodeId}")
    @Operation(summary = "Get cluster node by ID")
    public ResponseEntity<ClusterNodeEntity> getClusterNode(@PathVariable String nodeId) {
        return dataCenterService.getClusterNode(nodeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cluster/health")
    @Operation(summary = "Get cluster health status")
    public ResponseEntity<Map<String, Object>> getClusterHealth() {
        return ResponseEntity.ok(dataCenterService.getClusterHealth());
    }

    @PostMapping("/cluster/nodes/{nodeId}/drain")
    @Operation(summary = "Start node drain")
    public ResponseEntity<Void> startNodeDrain(@PathVariable String nodeId) {
        dataCenterService.startNodeDrain(nodeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cluster/nodes/{nodeId}/drain")
    @Operation(summary = "Stop node drain")
    public ResponseEntity<Void> stopNodeDrain(@PathVariable String nodeId) {
        dataCenterService.stopNodeDrain(nodeId);
        return ResponseEntity.ok().build();
    }

    // ==================== Cache Management ====================

    @GetMapping("/cache")
    @Operation(summary = "Get all cache metrics")
    public ResponseEntity<List<CacheMetricsEntity>> getCacheMetrics() {
        return ResponseEntity.ok(dataCenterService.getCacheMetrics());
    }

    @GetMapping("/cache/node/{nodeId}")
    @Operation(summary = "Get cache metrics by node")
    public ResponseEntity<List<CacheMetricsEntity>> getCacheMetricsByNode(@PathVariable String nodeId) {
        return ResponseEntity.ok(dataCenterService.getCacheMetricsByNode(nodeId));
    }

    @PostMapping("/cache/{cacheName}/clear")
    @Operation(summary = "Clear specific cache")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable String cacheName) {
        return ResponseEntity.ok(dataCenterService.clearCache(cacheName));
    }

    @PostMapping("/cache/clear-all")
    @Operation(summary = "Clear all caches")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        return ResponseEntity.ok(dataCenterService.clearAllCaches());
    }

    // ==================== Scheduled Jobs ====================

    @GetMapping("/jobs")
    @Operation(summary = "Get all scheduled jobs")
    public ResponseEntity<List<ScheduledJobEntity>> getScheduledJobs() {
        return ResponseEntity.ok(dataCenterService.getScheduledJobs());
    }

    @PostMapping("/jobs/{jobId}/run")
    @Operation(summary = "Run job now")
    public ResponseEntity<ScheduledJobEntity> runJobNow(@PathVariable String jobId) {
        return ResponseEntity.ok(dataCenterService.runJobNow(jobId));
    }

    @PostMapping("/jobs/{jobId}/enable")
    @Operation(summary = "Enable scheduled job")
    public ResponseEntity<ScheduledJobEntity> enableJob(@PathVariable String jobId) {
        return ResponseEntity.ok(dataCenterService.enableJob(jobId));
    }

    @PostMapping("/jobs/{jobId}/disable")
    @Operation(summary = "Disable scheduled job")
    public ResponseEntity<ScheduledJobEntity> disableJob(@PathVariable String jobId) {
        return ResponseEntity.ok(dataCenterService.disableJob(jobId));
    }

    // ==================== Services ====================

    @GetMapping("/services")
    @Operation(summary = "Get all services")
    public ResponseEntity<List<ServiceEntity>> getServices() {
        return ResponseEntity.ok(dataCenterService.getServices());
    }

    @PostMapping("/services/{serviceKey}/start")
    @Operation(summary = "Start service")
    public ResponseEntity<Void> startService(@PathVariable String serviceKey) {
        dataCenterService.startService(serviceKey);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/services/{serviceKey}/stop")
    @Operation(summary = "Stop service")
    public ResponseEntity<Void> stopService(@PathVariable String serviceKey) {
        dataCenterService.stopService(serviceKey);
        return ResponseEntity.ok().build();
    }

    // ==================== System Info ====================

    @GetMapping("/system-info")
    @Operation(summary = "Get system information")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        return ResponseEntity.ok(dataCenterService.getSystemInfo());
    }

    // ==================== Indexing ====================

    @GetMapping("/index")
    @Operation(summary = "Get index statistics")
    public ResponseEntity<List<IndexStatsEntity>> getIndexStats() {
        return ResponseEntity.ok(dataCenterService.getIndexStats());
    }

    @PostMapping("/index/reindex-all")
    @Operation(summary = "Start full reindex")
    public ResponseEntity<Map<String, Object>> reindexAll() {
        return ResponseEntity.ok(dataCenterService.reindexAll());
    }

    @PostMapping("/index/reindex-issues")
    @Operation(summary = "Start issue reindex")
    public ResponseEntity<Map<String, Object>> reindexIssues() {
        return ResponseEntity.ok(dataCenterService.reindexIssues());
    }

    @GetMapping("/index/progress")
    @Operation(summary = "Get indexing progress")
    public ResponseEntity<Map<String, Object>> getIndexingProgress() {
        return ResponseEntity.ok(dataCenterService.getIndexingProgress());
    }
}
