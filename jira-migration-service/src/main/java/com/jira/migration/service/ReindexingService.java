package com.jira.migration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Reindexing Service
 * Triggers re-indexing after successful import to ensure search functionality
 * Integrates with the search service to update Lucene/OpenSearch indexes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReindexingService {

    /**
     * Trigger re-indexing for all imported entities
     * Called after successful import completion
     */
    public CompletableFuture<ReindexResult> triggerReindexing(
            UUID jobId,
            ReindexConfig config,
            List<String> entityTypes) {

        log.info("Starting reindexing for job {}: entities={}", jobId, entityTypes);

        return CompletableFuture.supplyAsync(() -> {
            ReindexResult result = new ReindexResult();
            result.setJobId(jobId);
            result.setStartTime(System.currentTimeMillis());

            Map<String, ReindexStatus> statusByType = new HashMap<>();

            for (String entityType : entityTypes) {
                try {
                    log.info("Reindexing {}...", entityType);
                    ReindexStatus status = performReindex(entityType, config);
                    statusByType.put(entityType, status);
                } catch (Exception e) {
                    log.error("Failed to reindex {}: {}", entityType, e.getMessage());
                    statusByType.put(entityType, ReindexStatus.builder()
                            .entityType(entityType)
                            .status("FAILED")
                            .errorMessage(e.getMessage())
                            .build());
                }
            }

            result.setStatusByType(statusByType);
            result.setEndTime(System.currentTimeMillis());

            long totalIndexed = statusByType.values().stream()
                    .filter(s -> "COMPLETED".equals(s.getStatus()))
                    .mapToLong(s -> s.getIndexedCount())
                    .sum();

            result.setTotalIndexed(totalIndexed);

            boolean allSuccess = statusByType.values().stream()
                    .allMatch(s -> "COMPLETED".equals(s.getStatus()));

            result.setSuccess(allSuccess);

            log.info("Reindexing completed for job {}: indexed={}, success={}",
                    jobId, totalIndexed, allSuccess);

            return result;
        });
    }

    /**
     * Trigger incremental re-indexing for specific entities
     * Used when importing small batches
     */
    public CompletableFuture<Void> triggerIncrementalReindex(
            String entityType,
            List<UUID> entityIds) {

        log.info("Starting incremental reindex: type={}, count={}", entityType, entityIds.size());

        return CompletableFuture.runAsync(() -> {
            for (UUID entityId : entityIds) {
                try {
                    reindexEntity(entityType, entityId);
                } catch (Exception e) {
                    log.error("Failed to reindex {} {}: {}", entityType, entityId, e.getMessage());
                }
            }
        });
    }

    /**
     * Perform full reindex for an entity type
     */
    private ReindexStatus performReindex(String entityType, ReindexConfig config) {
        log.debug("Performing full reindex for: {}", entityType);

        ReindexStatus.ReindexStatusBuilder builder = ReindexStatus.builder();
        builder.entityType(entityType);
        builder.startTime(System.currentTimeMillis());

        try {
            // In production: Call search service REST API
            // POST /api/search/reindex
            // Body: { "entityType": entityType, "batchSize": config.batchSize }

            // Simulate reindexing
            long count = switch (entityType.toUpperCase()) {
                case "ISSUE" -> config.issueCount;
                case "PROJECT" -> config.projectCount;
                case "COMMENT" -> config.commentCount;
                case "WORKLOG" -> config.worklogCount;
                default -> 0;
            };

            // Simulate batching
            int batchSize = config.getBatchSize();
            int batches = (int) Math.ceil((double) count / batchSize);

            for (int i = 0; i < batches; i++) {
                log.debug("Reindex batch {}/{}", i + 1, batches);
                Thread.sleep(50); // Simulate work
            }

            builder.status("COMPLETED")
                    .indexedCount(count)
                    .batchesProcessed(batches);

        } catch (Exception e) {
            builder.status("FAILED")
                    .errorMessage(e.getMessage());
        }

        builder.endTime(System.currentTimeMillis());
        return builder.build();
    }

    /**
     * Reindex single entity
     */
    private void reindexEntity(String entityType, UUID entityId) {
        log.debug("Reindexing {} {}", entityType, entityId);
        // In production: Call search service to update single entity
        // POST /api/search/reindex/{entityType}/{entityId}
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReindexConfig {
        private int batchSize;
        private int parallelThreads;
        private boolean fullReindex;

        // Counts for simulation
        private int issueCount;
        private int projectCount;
        private int commentCount;
        private int worklogCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReindexStatus {
        private String entityType;
        private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED
        private long indexedCount;
        private int batchesProcessed;
        private String errorMessage;
        private long startTime;
        private long endTime;

        public long getDurationMs() {
            return endTime - startTime;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReindexResult {
        private UUID jobId;
        private boolean success;
        private long totalIndexed;
        private Map<String, ReindexStatus> statusByType;
        private long startTime;
        private long endTime;

        public long getDurationMs() {
            return endTime - startTime;
        }
    }
}