package com.avionics_systems.admin.service;

import com.avionics_systems.admin.entity.*;
import com.avionics_systems.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Center Administration Service - Cluster, cache, indexing, services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataCenterService {

    private final ClusterNodeRepository clusterNodeRepository;
    private final CacheMetricsRepository cacheMetricsRepository;
    private final ScheduledJobRepository scheduledJobRepository;
    private final ServiceRepository serviceRepository;
    private final SystemInfoRepository systemInfoRepository;
    private final IndexStatsRepository indexStatsRepository;
    private final AuditLogRepository auditLogRepository;
    private final MessageSource messageSource;

    @Value("${app.defaults.audit-severity:INFO}")
    private String defaultAuditSeverity;

    @Value("${app.defaults.audit-source:UI}")
    private String defaultAuditSource;

    // ==================== Cluster Nodes ====================

    @Transactional(readOnly = true)
    public List<ClusterNodeEntity> getClusterNodes() {
        return clusterNodeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ClusterNodeEntity> getClusterNode(String nodeId) {
        return clusterNodeRepository.findById(nodeId);
    }

    @Transactional
    public Map<String, Object> getClusterHealth() {
        List<ClusterNodeEntity> nodes = clusterNodeRepository.findAll();

        Map<String, Object> health = new HashMap<>();
        health.put("clusterState", "RUNNING");
        health.put("nodeCount", nodes.size());
        health.put("activeNodes", nodes.stream().filter(n -> "ACTIVE".equals(n.getNodeState())).count());
        health.put("inactiveNodes", nodes.stream().filter(n -> !"ACTIVE".equals(n.getNodeState())).count());

        List<Map<String, Object>> nodeHealth = new ArrayList<>();
        for (ClusterNodeEntity node : nodes) {
            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("nodeId", node.getNodeId());
            nodeInfo.put("nodeName", node.getNodeName());
            nodeInfo.put("state", node.getNodeState());
            nodeInfo.put("cpuUsage", node.getCpuUsage());
            nodeInfo.put("memoryUsage", node.getMemoryUsage());
            nodeInfo.put("isHealthy", "ACTIVE".equals(node.getNodeState()));
            nodeHealth.add(nodeInfo);
        }
        health.put("nodes", nodeHealth);

        return health;
    }

    @Transactional
    public void startNodeDrain(String nodeId) {
        ClusterNodeEntity node = clusterNodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.node.not.found", null, Locale.ENGLISH)));

        node.setNodeState("DRAINING");
        clusterNodeRepository.save(node);

        logAudit("NODE_DRAIN_START", "CLUSTER", nodeId, node.getNodeName(), "Node drain started");
    }

    @Transactional
    public void stopNodeDrain(String nodeId) {
        ClusterNodeEntity node = clusterNodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.node.not.found", null, Locale.ENGLISH)));

        node.setNodeState("ACTIVE");
        clusterNodeRepository.save(node);

        logAudit("NODE_DRAIN_STOP", "CLUSTER", nodeId, node.getNodeName(), "Node drain stopped");
    }

    // ==================== Cache Management ====================

    @Transactional(readOnly = true)
    public List<CacheMetricsEntity> getCacheMetrics() {
        return cacheMetricsRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CacheMetricsEntity> getCacheMetricsByNode(String nodeId) {
        return cacheMetricsRepository.findByNodeId(nodeId);
    }

    @Transactional
    public Map<String, Object> clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        logAudit("CACHE_CLEAR", "SYSTEM", cacheName, cacheName, "Cache cleared");

        Map<String, Object> result = new HashMap<>();
        result.put("cacheName", cacheName);
        result.put("cleared", true);
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    @Transactional
    public Map<String, Object> clearAllCaches() {
        List<CacheMetricsEntity> caches = cacheMetricsRepository.findAll();
        List<String> cleared = new ArrayList<>();

        for (CacheMetricsEntity cache : caches) {
            cleared.add(cache.getCacheName());
        }

        log.info("Clearing all {} caches", cleared.size());
        logAudit("CACHE_CLEAR_ALL", "SYSTEM", null, "All Caches", "All caches cleared");

        Map<String, Object> result = new HashMap<>();
        result.put("cachesCleared", cleared);
        result.put("count", cleared.size());
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    // ==================== Scheduled Jobs ====================

    @Transactional(readOnly = true)
    public List<ScheduledJobEntity> getScheduledJobs() {
        return scheduledJobRepository.findAll();
    }

    @Transactional
    public ScheduledJobEntity runJobNow(String jobId) {
        ScheduledJobEntity job = scheduledJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.job.not.found", null, Locale.ENGLISH)));

        job.setIsRunning(true);
        job.setLastRunAt(LocalDateTime.now());
        scheduledJobRepository.save(job);

        log.info("Running job manually: {}", jobId);
        logAudit("JOB_RUN", "SCHEDULER", jobId, job.getJobName(), "Job run manually");

        // In real implementation, would execute the job
        job.setIsRunning(false);
        job.setLastDurationMs(100L);
        job.setLastRunAt(LocalDateTime.now());

        return scheduledJobRepository.save(job);
    }

    @Transactional
    public ScheduledJobEntity enableJob(String jobId) {
        ScheduledJobEntity job = scheduledJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.job.not.found", null, Locale.ENGLISH)));

        job.setIsEnabled(true);
        scheduledJobRepository.save(job);

        logAudit("JOB_ENABLE", "SCHEDULER", jobId, job.getJobName(), "Job enabled");

        return job;
    }

    @Transactional
    public ScheduledJobEntity disableJob(String jobId) {
        ScheduledJobEntity job = scheduledJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.job.not.found", null, Locale.ENGLISH)));

        job.setIsEnabled(false);
        scheduledJobRepository.save(job);

        logAudit("JOB_DISABLE", "SCHEDULER", jobId, job.getJobName(), "Job disabled");

        return job;
    }

    // ==================== Services ====================

    @Transactional(readOnly = true)
    public List<ServiceEntity> getServices() {
        return serviceRepository.findAll();
    }

    @Transactional
    public void startService(String serviceKey) {
        ServiceEntity service = serviceRepository.findByServiceName(serviceKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.service.not.found", null, Locale.ENGLISH)));

        service.setIsRunning(true);
        service.setLastStartedAt(LocalDateTime.now());
        serviceRepository.save(service);

        logAudit("SERVICE_START", "SYSTEM", serviceKey, service.getServiceName(), "Service started");
    }

    @Transactional
    public void stopService(String serviceKey) {
        ServiceEntity service = serviceRepository.findByServiceName(serviceKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.service.not.found", null, Locale.ENGLISH)));

        service.setIsRunning(false);
        serviceRepository.save(service);

        logAudit("SERVICE_STOP", "SYSTEM", serviceKey, service.getServiceName(), "Service stopped");
    }

    // ==================== System Info ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemInfo() {
        List<SystemInfoEntity> infoList = systemInfoRepository.findAll();
        Map<String, Object> info = new HashMap<>();

        for (SystemInfoEntity entity : infoList) {
            if (!entity.getKey().startsWith("password")) {
                info.put(entity.getKey(), entity.getValue());
            }
        }

        // Add computed system info
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("totalMemory", Runtime.getRuntime().totalMemory());
        info.put("freeMemory", Runtime.getRuntime().freeMemory());
        info.put("maxMemory", Runtime.getRuntime().maxMemory());
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());

        return info;
    }

    // ==================== Indexing ====================

    @Transactional(readOnly = true)
    public List<IndexStatsEntity> getIndexStats() {
        return indexStatsRepository.findAll();
    }

    @Transactional
    public Map<String, Object> reindexAll() {
        log.info("Starting full reindex");
        logAudit("REINDEX_ALL", "INDEXING", null, "Full Index", "Full reindex started");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "STARTED");
        result.put("type", "FULL");
        result.put("startedAt", LocalDateTime.now());
        result.put("estimatedDuration", "15-30 minutes");

        return result;
    }

    @Transactional
    public Map<String, Object> reindexIssues() {
        log.info("Starting issue reindex");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "STARTED");
        result.put("type", "ISSUES");
        result.put("startedAt", LocalDateTime.now());

        return result;
    }

    @Transactional
    public Map<String, Object> getIndexingProgress() {
        Map<String, Object> progress = new HashMap<>();
        progress.put("currentStatus", "IDLE");
        progress.put("documentsIndexed", 0);
        progress.put("totalDocuments", 0);
        progress.put("percentage", 0);
        progress.put("lastIndexedAt", LocalDateTime.now());
        return progress;
    }

    // ==================== Helper Methods ====================

    private void logAudit(String action, String category, String entityId, String entityName, String details) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .timestamp(LocalDateTime.now())
                .action(action)
                .category(category)
                .entityType(category)
                .entityId(entityId)
                .entityName(entityName)
                .details(details)
                .result("SUCCESS")
                .severity(defaultAuditSeverity)
                .source(defaultAuditSource)
                .build();
        auditLogRepository.save(auditLog);
    }
}
