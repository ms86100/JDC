package com.jira.migration.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.config.DlqConfig;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.exception.DlqOperationException;
import com.jira.migration.repository.EntityStatusRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dead Letter Queue service for handling failed operations.
 * Stores failed operations for later review, retry, or manual intervention.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final DlqConfig dlqConfig;
    private final EntityStatusRepository entityStatusRepository;
    private final ObjectMapper objectMapper;

    // In-memory storage for DLQ entries (in production, this would be persisted)
    private final Map<String, FailedOperation> dlqEntries = new ConcurrentHashMap<>();
    private final Map<String, Instant> retryTimestamps = new ConcurrentHashMap<>();

    /**
     * Add a failed operation to the DLQ.
     */
    @Transactional
    public void enqueue(FailedOperation failedOperation) {
        if (!dlqConfig.isEnabled()) {
            log.warn("DLQ is disabled, discarding failed operation: {}", failedOperation.getOperationType());
            return;
        }

        String dlqId = failedOperation.getId() != null ? failedOperation.getId() : generateDlqId();

        // Check queue size limit
        if (dlqEntries.size() >= dlqConfig.getMaxQueueSize()) {
            evictOldestEntry();
        }

        failedOperation.setId(dlqId);
        failedOperation.setFirstFailure(failedOperation.getFirstFailure() != null ?
                failedOperation.getFirstFailure() : Instant.now());
        failedOperation.setLastAttempt(Instant.now());

        dlqEntries.put(dlqId, failedOperation);

        // Also persist to entity_status table
        persistFailedOperation(failedOperation);

        log.info("Enqueued to DLQ: id={}, operation={}, entityType={}, attemptCount={}",
                dlqId, failedOperation.getOperationType(), failedOperation.getEntityType(),
                failedOperation.getAttemptCount());

        // Check for auto-retry eligibility
        if (dlqConfig.isAutoRetry() && shouldAutoRetry(failedOperation)) {
            scheduleAutoRetry(dlqId);
        }
    }

    /**
     * Enqueue from an exception context.
     */
    public void enqueue(String operationType, String entityType, String payload,
                        Exception error, Map<String, Object> metadata) {
        FailedOperation operation = FailedOperation.builder()
                .operationType(operationType)
                .entityType(entityType)
                .payload(payload)
                .errorMessage(error != null ? error.getMessage() : "Unknown error")
                .errorStackTrace(error != null ? getStackTrace(error) : null)
                .metadata(metadata != null ? metadata : Collections.emptyMap())
                .build();

        enqueue(operation);
    }

    /**
     * Get all pending DLQ operations with pagination.
     */
    public List<FailedOperation> getPending(int page, int pageSize) {
        return dlqEntries.values().stream()
                .filter(op -> "PENDING".equals(op.getStatus()) || "SCHEDULED".equals(op.getStatus()))
                .sorted(Comparator.comparing(FailedOperation::getFirstFailure))
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    /**
     * Get all DLQ operations for a specific job.
     */
    public List<FailedOperation> getByJobId(String jobId) {
        return dlqEntries.values().stream()
                .filter(op -> jobId.equals(op.getMetadata().get("jobId")))
                .sorted(Comparator.comparing(FailedOperation::getFirstFailure))
                .collect(Collectors.toList());
    }

    /**
     * Get a specific DLQ entry by ID.
     */
    public Optional<FailedOperation> get(String dlqId) {
        return Optional.ofNullable(dlqEntries.get(dlqId));
    }

    /**
     * Retry a specific DLQ operation.
     */
    @Transactional
    public RetryResult retry(String dlqId) {
        FailedOperation operation = dlqEntries.get(dlqId);

        if (operation == null) {
            throw new DlqOperationException("DLQ entry not found", dlqId, "RETRY", "Entry does not exist");
        }

        log.info("Retrying DLQ entry: {} (attempt {})", dlqId, operation.getAttemptCount() + 1);

        RetryResult result = RetryResult.builder()
                .dlqId(dlqId)
                .operationType(operation.getOperationType())
                .entityType(operation.getEntityType())
                .attemptCount(operation.getAttemptCount())
                .build();

        try {
            // Update attempt tracking
            operation.setAttemptCount(operation.getAttemptCount() + 1);
            operation.setLastAttempt(Instant.now());
            operation.setStatus("RETRYING");

            // In production, this would call the actual retry logic
            // For now, simulate a retry attempt
            boolean success = simulateRetry(operation);

            if (success) {
                operation.setStatus("COMPLETED");
                result.setSuccess(true);
                log.info("DLQ retry successful for: {}", dlqId);
            } else {
                operation.setStatus("PENDING");
                result.setSuccess(false);
                result.setErrorMessage("Retry simulation failed");
                log.warn("DLQ retry failed for: {}", dlqId);
            }

        } catch (Exception e) {
            operation.setStatus("PENDING");
            operation.setLastError(e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("DLQ retry exception for {}: {}", dlqId, e.getMessage());
        }

        // Update entity status
        UUID jobId = operation.getMetadata().get("jobId") != null ?
                UUID.fromString(operation.getMetadata().get("jobId").toString()) : null;
        updateEntityStatus(operation, result.isSuccess(), jobId);

        return result;
    }

    /**
     * Retry all pending DLQ operations.
     */
    @Transactional
    public RetrySummary retryAll() {
        List<FailedOperation> pending = getPending(0, dlqConfig.getMaxQueueSize());

        RetrySummary summary = RetrySummary.builder()
                .totalPending(pending.size())
                .build();

        for (FailedOperation operation : pending) {
            try {
                RetryResult result = retry(operation.getId());
                if (result.isSuccess()) {
                    summary.incrementSuccess();
                } else {
                    summary.incrementFailed();
                    summary.addError(result.getErrorMessage());
                }
            } catch (Exception e) {
                summary.incrementFailed();
                summary.addError(e.getMessage());
            }
        }

        log.info("DLQ batch retry completed: success={}, failed={}", summary.getSuccessCount(), summary.getFailedCount());

        return summary;
    }

    /**
     * Discard a DLQ entry.
     */
    @Transactional
    public void discard(String dlqId, String reason) {
        FailedOperation operation = dlqEntries.get(dlqId);

        if (operation == null) {
            throw new DlqOperationException("DLQ entry not found", dlqId, "DISCARD", "Entry does not exist");
        }

        operation.setStatus("DISCARDED");
        operation.setDiscardReason(reason);
        operation.setDiscardedAt(Instant.now());

        log.info("Discarded DLQ entry {}: {}", dlqId, reason);

        // Update entity status
        UUID jobId = operation.getMetadata().get("jobId") != null ?
                UUID.fromString(operation.getMetadata().get("jobId").toString()) : null;
        updateEntityStatus(operation, false, jobId);
    }

    /**
     * Get DLQ statistics.
     */
    public DLQStatistics getStatistics() {
        Map<String, Long> byOperationType = dlqEntries.values().stream()
                .collect(Collectors.groupingBy(FailedOperation::getOperationType, Collectors.counting()));

        Map<String, Long> byEntityType = dlqEntries.values().stream()
                .collect(Collectors.groupingBy(FailedOperation::getEntityType, Collectors.counting()));

        Map<String, Long> byStatus = dlqEntries.values().stream()
                .collect(Collectors.groupingBy(FailedOperation::getStatus, Collectors.counting()));

        long totalRetries = dlqEntries.values().stream()
                .mapToLong(FailedOperation::getAttemptCount)
                .sum();

        Instant oldestEntry = dlqEntries.values().stream()
                .map(FailedOperation::getFirstFailure)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);

        return DLQStatistics.builder()
                .totalEntries(dlqEntries.size())
                .pendingCount((int) dlqEntries.values().stream()
                        .filter(op -> "PENDING".equals(op.getStatus()) || "SCHEDULED".equals(op.getStatus()))
                        .count())
                .byOperationType(byOperationType)
                .byEntityType(byEntityType)
                .byStatus(byStatus)
                .totalRetryAttempts(totalRetries)
                .oldestEntry(oldestEntry)
                .queueCapacity(dlqConfig.getMaxQueueSize())
                .queueUsagePercentage((double) dlqEntries.size() / dlqConfig.getMaxQueueSize() * 100)
                .build();
    }

    /**
     * Check if a DLQ entry is eligible for auto-retry.
     */
    private boolean shouldAutoRetry(FailedOperation operation) {
        return operation.getAttemptCount() < dlqConfig.getMaxAutoRetryAttempts() &&
               (operation.getLastAttempt() == null ||
                operation.getLastAttempt().isBefore(Instant.now().minusSeconds(
                        dlqConfig.getAutoRetryDelayHours() * 3600L)));
    }

    /**
     * Schedule an auto-retry.
     */
    private void scheduleAutoRetry(String dlqId) {
        Instant retryTime = Instant.now().plusSeconds(dlqConfig.getAutoRetryDelayHours() * 3600L);
        retryTimestamps.put(dlqId, retryTime);

        FailedOperation op = dlqEntries.get(dlqId);
        if (op != null) {
            op.setStatus("SCHEDULED");
            op.setScheduledRetryTime(retryTime);
        }

        log.debug("Scheduled auto-retry for DLQ entry {} at {}", dlqId, retryTime);
    }

    /**
     * Scheduled task to process auto-retries.
     */
    @Scheduled(fixedDelayString = "${dlq.auto-retry-check-interval-ms:60000}")
    public void processScheduledRetries() {
        Instant now = Instant.now();

        retryTimestamps.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(now))
                .forEach(entry -> {
                    log.info("Processing scheduled retry for DLQ entry: {}", entry.getKey());
                    retry(entry.getKey());
                    retryTimestamps.remove(entry.getKey());
                });
    }

    /**
     * Scheduled task to clean up expired DLQ entries.
     */
    @Scheduled(cron = "${dlq.cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void cleanupExpiredEntries() {
        if (!dlqConfig.isCleanupEnabled()) {
            return;
        }

        Instant cutoff = Instant.now().minusMillis(dlqConfig.getRetentionDurationMs());

        List<String> expiredIds = dlqEntries.values().stream()
                .filter(op -> op.getFirstFailure() != null && op.getFirstFailure().isBefore(cutoff))
                .map(FailedOperation::getId)
                .collect(Collectors.toList());

        for (String id : expiredIds) {
            FailedOperation removed = dlqEntries.remove(id);
            if (removed != null) {
                log.info("Cleaned up expired DLQ entry: {}", id);
            }
        }

        if (!expiredIds.isEmpty()) {
            log.info("Cleaned up {} expired DLQ entries", expiredIds.size());
        }
    }

    private String generateDlqId() {
        return "DLQ-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void evictOldestEntry() {
        dlqEntries.values().stream()
                .min(Comparator.comparing(FailedOperation::getFirstFailure))
                .ifPresent(oldest -> {
                    dlqEntries.remove(oldest.getId());
                    log.warn("Evicted oldest DLQ entry due to size limit: {}", oldest.getId());
                });
    }

    private void persistFailedOperation(FailedOperation operation) {
        // Store reference in entity_status for tracking
        try {
            EntityStatus entityStatus = EntityStatus.builder()
                    .jobId(operation.getMetadata().get("jobId") != null ?
                            UUID.fromString(operation.getMetadata().get("jobId").toString()) : null)
                    .entityType(operation.getEntityType())
                    .entityKey(operation.getMetadata().get("entityKey") != null ?
                            operation.getMetadata().get("entityKey").toString() : null)
                    .status("DLQ")
                    .errorMessage(operation.getErrorMessage())
                    .errorContext(operation.getPayload())
                    .build();
            entityStatusRepository.save(entityStatus);
        } catch (Exception e) {
            log.warn("Failed to persist DLQ entry to database: {}", e.getMessage());
        }
    }

    private void updateEntityStatus(FailedOperation operation, boolean success, UUID jobId) {
        try {
            String entityKey = operation.getMetadata().get("entityKey") != null ?
                    operation.getMetadata().get("entityKey").toString() : null;
            String entityType = operation.getEntityType();

            if (entityKey != null && jobId != null) {
                // Update entity status for the failed entity
                entityStatusRepository.findByJobIdAndEntityTypeAndSourceIdentifier(
                        jobId, entityType, entityKey).ifPresent(status -> {
                    status.setStatus(success ? "RETRY_SUCCESS" : "RETRY_FAILED");
                    status.setProcessedAt(java.time.LocalDateTime.now());
                    entityStatusRepository.save(status);
                });
            }
        } catch (Exception e) {
            log.warn("Failed to update entity status after DLQ operation: {}", e.getMessage());
        }
    }

    private boolean simulateRetry(FailedOperation operation) {
        // In production, this would actually retry the operation
        // For now, return true for entries with low attempt count
        return operation.getAttemptCount() < 2;
    }

    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Failed operation record stored in the DLQ.
     */
    @Data
    @Builder
    public static class FailedOperation {
        private String id;
        private String operationType;
        private String entityType;
        private String payload;
        private String errorMessage;
        private String errorStackTrace;
        @Builder.Default
        private int attemptCount = 0;
        private Instant firstFailure;
        private Instant lastAttempt;
        private Instant scheduledRetryTime;
        private String lastError;
        private String status;
        private String discardReason;
        private Instant discardedAt;
        @Builder.Default
        private Map<String, Object> metadata = new HashMap<>();
    }

    /**
     * Result of a DLQ retry operation.
     */
    @Data
    @Builder
    public static class RetryResult {
        private String dlqId;
        private String operationType;
        private String entityType;
        @Builder.Default
        private boolean success = false;
        private String errorMessage;
        @Builder.Default
        private int attemptCount = 0;
        private Object result;
    }

    /**
     * Summary of a batch retry operation.
     */
    @Data
    @Builder
    public static class RetrySummary {
        @Builder.Default
        private int totalPending = 0;
        @Builder.Default
        private int successCount = 0;
        @Builder.Default
        private int failedCount = 0;
        @Builder.Default
        private List<String> errors = new ArrayList<>();
        private long durationMs;

        public void incrementSuccess() {
            successCount++;
        }

        public void incrementFailed() {
            failedCount++;
        }

        public void addError(String error) {
            if (error != null && errors.size() < 100) { // Limit error list size
                errors.add(error);
            }
        }
    }

    /**
     * Statistics about the DLQ.
     */
    @Data
    @Builder
    public static class DLQStatistics {
        private long totalEntries;
        private int pendingCount;
        private Map<String, Long> byOperationType;
        private Map<String, Long> byEntityType;
        private Map<String, Long> byStatus;
        private long totalRetryAttempts;
        private Instant oldestEntry;
        private int queueCapacity;
        private double queueUsagePercentage;
    }
}