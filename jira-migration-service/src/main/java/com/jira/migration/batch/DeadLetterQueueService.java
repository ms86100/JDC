package com.jira.migration.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.config.DlqConfig;
import com.jira.migration.entity.DlqEntry;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.exception.DlqOperationException;
import com.jira.migration.repository.DlqEntryRepository;
import com.jira.migration.repository.EntityStatusRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dead Letter Queue service for handling failed operations.
 * Provides persistent storage with retry, discard, and resolution tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final DlqConfig dlqConfig;
    private final DlqEntryRepository dlqEntryRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final ObjectMapper objectMapper;
    private final DlqRetryExecutor dlqRetryExecutor;

    // Legacy in-memory cache for fast lookups (populated from DB on startup)
    private final Map<String, FailedOperation> memoryCache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    /**
     * Initialize memory cache from database on startup.
     */
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    @SchedulerLock(name = "DeadLetterQueueService_initializeFromDatabase", lockAtMostFor = "PT1M", lockAtLeastFor = "PT10S")
    public void initializeFromDatabase() {
        if (initialized) return;

        log.info("Initializing DLQ memory cache from database...");
        List<DlqEntry> pendingEntries = dlqEntryRepository.findPending(PageRequest.of(0, dlqConfig.getMaxQueueSize())).getContent();

        for (DlqEntry entry : pendingEntries) {
            FailedOperation operation = toFailedOperation(entry);
            memoryCache.put(entry.getId().toString(), operation);
        }

        initialized = true;
        log.info("DLQ memory cache initialized with {} entries", memoryCache.size());
    }

    // ========================================================================
    // ENQUEUE OPERATIONS
    // ========================================================================

    /**
     * Add a failed operation to the DLQ with persistence.
     */
    @Transactional
    public void enqueue(FailedOperation failedOperation) {
        if (!dlqConfig.isEnabled()) {
            log.warn("DLQ is disabled, discarding failed operation: {}", failedOperation.getOperationType());
            return;
        }

        String dlqId = failedOperation.getId() != null ? failedOperation.getId() : generateDlqId();

        // Enforce queue size limit
        if (memoryCache.size() >= dlqConfig.getMaxQueueSize()) {
            evictOldestEntry();
        }

        failedOperation.setId(dlqId);
        failedOperation.setFirstFailure(failedOperation.getFirstFailure() != null ?
                failedOperation.getFirstFailure() : Instant.now());
        failedOperation.setLastAttempt(Instant.now());

        // Persist to database
        DlqEntry entry = toDlqEntry(failedOperation);
        entry = dlqEntryRepository.save(entry);

        // Update memory cache
        memoryCache.put(entry.getId().toString(), failedOperation);

        log.info("Enqueued to DLQ: id={}, operation={}, entityType={}, attemptCount={}",
                entry.getId(), failedOperation.getOperationType(), failedOperation.getEntityType(),
                failedOperation.getAttemptCount());

        // Check for auto-retry eligibility
        if (dlqConfig.isAutoRetry() && shouldAutoRetry(failedOperation)) {
            scheduleAutoRetry(entry.getId().toString());
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

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Get all pending DLQ operations with pagination.
     */
    public List<FailedOperation> getPending(int page, int pageSize) {
        Page<DlqEntry> entries = dlqEntryRepository.findPending(PageRequest.of(page, pageSize));
        return entries.getContent().stream()
                .map(this::toFailedOperation)
                .collect(Collectors.toList());
    }

    /**
     * Get all DLQ operations for a specific job.
     */
    public List<FailedOperation> getByJobId(String jobId) {
        try {
            UUID jobUuid = UUID.fromString(jobId);
            List<DlqEntry> entries = dlqEntryRepository.findPendingByJobId(jobUuid);
            return entries.stream()
                    .map(this::toFailedOperation)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return memoryCache.values().stream()
                    .filter(op -> jobId.equals(op.getMetadata().get("jobId")))
                    .sorted(Comparator.comparing(FailedOperation::getFirstFailure))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get a specific DLQ entry by ID.
     */
    public Optional<FailedOperation> get(String dlqId) {
        // Check memory cache first
        FailedOperation cached = memoryCache.get(dlqId);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Fall back to database
        try {
            UUID id = UUID.fromString(dlqId);
            return dlqEntryRepository.findById(id)
                    .map(this::toFailedOperation);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ========================================================================
    // RETRY OPERATIONS
    // ========================================================================

    /**
     * Retry a specific DLQ operation with exponential backoff.
     */
    @Transactional
    public RetryResult retry(String dlqId) {
        FailedOperation operation = memoryCache.get(dlqId);

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

            // Perform actual retry with backoff
            boolean success = performRetry(operation);

            if (success) {
                operation.setStatus("COMPLETED");
                result.setSuccess(true);
                log.info("DLQ retry successful for: {}", dlqId);
            } else {
                operation.setStatus("PENDING");
                result.setSuccess(false);
                result.setErrorMessage("Retry operation returned false");
                log.warn("DLQ retry failed for: {}", dlqId);
            }

        } catch (OptimisticLockingFailureException e) {
            // Concurrent modification, retry later
            operation.setStatus("PENDING");
            operation.setLastError("Concurrent modification detected");
            result.setSuccess(false);
            result.setErrorMessage("Concurrent modification, please retry");
            log.warn("DLQ retry conflict for {}: concurrent modification", dlqId);
        } catch (Exception e) {
            operation.setStatus("PENDING");
            operation.setLastError(e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("DLQ retry exception for {}: {}", dlqId, e.getMessage());
        }

        // Update database record
        persistOperation(operation);

        // Update entity status
        UUID jobId = extractJobId(operation);
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
                    if (result.getErrorMessage() != null) {
                        summary.addError(result.getErrorMessage());
                    }
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
     * Perform actual retry with exponential backoff.
     */
    private boolean performRetry(FailedOperation operation) {
        int attempt = operation.getAttemptCount();
        int maxAttempts = dlqConfig.getMaxAutoRetryAttempts();

        if (attempt > maxAttempts) {
            log.warn("DLQ entry {} exceeded max attempts ({})", operation.getId(), maxAttempts);
            return false;
        }

        // Exponential backoff: 1s, 2s, 4s, 8s, 16s...
        long delayMs = (long) Math.pow(2, attempt - 1) * 1000;
        long maxDelayMs = dlqConfig.getAutoRetryDelayHours() * 3600 * 1000L;
        delayMs = Math.min(delayMs, maxDelayMs);

        if (delayMs > 0) {
            try {
                log.debug("DLQ retry backoff for {}: {}ms", operation.getId(), delayMs);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        // Execute the retry operation based on type
        return switch (operation.getOperationType()) {
            case "CREATE_ISSUE" -> retryCreateIssue(operation);
            case "UPDATE_ISSUE" -> retryUpdateIssue(operation);
            case "CREATE_PROJECT" -> retryCreateProject(operation);
            case "CREATE_USER" -> retryCreateUser(operation);
            case "CREATE_ATTACHMENT" -> retryCreateAttachment(operation);
            case "CREATE_COMMENT" -> retryCreateComment(operation);
            case "MIGRATE_FIELD" -> retryMigrateField(operation);
            default -> {
                log.warn("Unknown operation type for retry: {}", operation.getOperationType());
                yield false;
            }
        };
    }

    /**
     * Retry create issue operation.
     */
    private boolean retryCreateIssue(FailedOperation operation) {
        return dlqRetryExecutor.retry(operation);
    }

    private boolean retryUpdateIssue(FailedOperation operation) {
        return dlqRetryExecutor.retry(operation);
    }

    private boolean retryCreateProject(FailedOperation operation) {
        return dlqRetryExecutor.retry(operation);
    }

    private boolean retryCreateUser(FailedOperation operation) {
        return dlqRetryExecutor.retry(operation);
    }

    private boolean retryCreateAttachment(FailedOperation operation) {
        return dlqRetryExecutor.retry(operation);
    }

    private boolean retryCreateComment(FailedOperation operation) {
        return dlqRetryExecutor.retry(operation);
    }

    private boolean retryMigrateField(FailedOperation operation) {
        return dlqRetryExecutor.retry(operation);
    }

    // ========================================================================
    // DISCARD OPERATIONS
    // ========================================================================

    /**
     * Discard a DLQ entry.
     */
    @Transactional
    public void discard(String dlqId, String reason) {
        FailedOperation operation = memoryCache.get(dlqId);

        if (operation == null) {
            throw new DlqOperationException("DLQ entry not found", dlqId, "DISCARD", "Entry does not exist");
        }

        operation.setStatus("DISCARDED");
        operation.setDiscardReason(reason);
        operation.setDiscardedAt(Instant.now());

        // Persist to database
        persistOperation(operation);

        log.info("Discarded DLQ entry {}: {}", dlqId, reason);

        // Update entity status
        UUID jobId = extractJobId(operation);
        updateEntityStatus(operation, false, jobId);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get DLQ statistics.
     */
    public DLQStatistics getStatistics() {
        Map<String, Long> byOperationType = new HashMap<>();
        Map<String, Long> byEntityType = new HashMap<>();
        Map<String, Long> byStatus = new HashMap<>();
        long totalRetries = 0;
        LocalDateTime oldestEntryTs = null;
        Instant oldestEntryInstant = null;

        List<DlqEntry> allEntries = dlqEntryRepository.findAll();
        for (DlqEntry entry : allEntries) {
            byOperationType.merge(entry.getOperationType(), 1L, Long::sum);
            byEntityType.merge(entry.getEntityType(), 1L, Long::sum);
            byStatus.merge(entry.getStatus().name(), 1L, Long::sum);
            totalRetries += entry.getAttemptCount();
            if (oldestEntryTs == null || entry.getFirstFailure().isBefore(oldestEntryTs)) {
                oldestEntryTs = entry.getFirstFailure();
            }
        }

        if (oldestEntryTs != null) {
            oldestEntryInstant = oldestEntryTs.toInstant(ZoneOffset.UTC);
        }

        long pendingCount = dlqEntryRepository.countPending();
        int totalEntries = allEntries.size();

        return DLQStatistics.builder()
                .totalEntries(totalEntries)
                .pendingCount((int) pendingCount)
                .byOperationType(byOperationType)
                .byEntityType(byEntityType)
                .byStatus(byStatus)
                .totalRetryAttempts(totalRetries)
                .oldestEntry(oldestEntryInstant)
                .queueCapacity(dlqConfig.getMaxQueueSize())
                .queueUsagePercentage(totalEntries > 0 ? (double) totalEntries / dlqConfig.getMaxQueueSize() * 100 : 0)
                .build();
    }

    // ========================================================================
    // SCHEDULED OPERATIONS
    // ========================================================================

    /**
     * Scheduled task to process auto-retries.
     */
    @Scheduled(fixedDelayString = "${dlq.auto-retry-check-interval-ms:60000}")
    @SchedulerLock(name = "DeadLetterQueueService_processScheduledRetries", lockAtMostFor = "PT48S", lockAtLeastFor = "PT24S")
    @Transactional
    public void processScheduledRetries() {
        List<DlqEntry> eligible = dlqEntryRepository.findEligibleForRetry(LocalDateTime.now());

        for (DlqEntry entry : eligible) {
            try {
                log.info("Processing scheduled retry for DLQ entry: {}", entry.getId());
                retry(entry.getId().toString());
            } catch (Exception e) {
                log.error("Failed to process scheduled retry for {}: {}", entry.getId(), e.getMessage());
            }
        }

        if (!eligible.isEmpty()) {
            log.info("Processed {} scheduled DLQ retries", eligible.size());
        }
    }

    /**
     * Scheduled task to clean up expired DLQ entries.
     */
    @Scheduled(cron = "${dlq.cleanup-cron:0 0 2 * * *}")
    @SchedulerLock(name = "DeadLetterQueueService_cleanupExpiredEntries", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    @Transactional
    public void cleanupExpiredEntries() {
        if (!dlqConfig.isCleanupEnabled()) {
            return;
        }

        long retentionMs = dlqConfig.getRetentionDurationMs();
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(retentionMs * 1_000_000);

        int deleted = dlqEntryRepository.deleteOldEntries(cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} expired DLQ entries", deleted);
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private boolean shouldAutoRetry(FailedOperation operation) {
        return operation.getAttemptCount() < dlqConfig.getMaxAutoRetryAttempts() &&
               (operation.getLastAttempt() == null ||
                operation.getLastAttempt().isBefore(Instant.now().minusSeconds(
                        dlqConfig.getAutoRetryDelayHours() * 3600L)));
    }

    private void scheduleAutoRetry(String dlqId) {
        long delaySeconds = dlqConfig.getAutoRetryDelayHours() * 3600L;
        Instant retryTime = Instant.now().plusSeconds(delaySeconds);

        FailedOperation op = memoryCache.get(dlqId);
        if (op != null) {
            op.setStatus("SCHEDULED");
            op.setScheduledRetryTime(retryTime);
        }

        // Persist schedule to database
        try {
            UUID id = UUID.fromString(dlqId);
            dlqEntryRepository.findById(id).ifPresent(entry -> {
                entry.setStatus(DlqEntry.DlqStatus.SCHEDULED);
                entry.setNextRetry(LocalDateTime.now().plusSeconds(delaySeconds));
                dlqEntryRepository.save(entry);
            });
        } catch (Exception e) {
            log.warn("Failed to persist scheduled retry: {}", e.getMessage());
        }

        log.debug("Scheduled auto-retry for DLQ entry {} at {}", dlqId, retryTime);
    }

    private void evictOldestEntry() {
        memoryCache.values().stream()
                .min(Comparator.comparing(FailedOperation::getFirstFailure))
                .ifPresent(oldest -> {
                    memoryCache.remove(oldest.getId());
                    log.warn("Evicted oldest DLQ entry due to size limit: {}", oldest.getId());
                });
    }

    private void persistOperation(FailedOperation operation) {
        try {
            DlqEntry entry = toDlqEntry(operation);
            if (operation.getId() != null) {
                try {
                    UUID id = UUID.fromString(operation.getId());
                    dlqEntryRepository.findById(id).ifPresent(existing -> {
                        copyToExisting(existing, entry);
                        dlqEntryRepository.save(existing);
                    });
                } catch (IllegalArgumentException e) {
                    dlqEntryRepository.save(entry);
                }
            } else {
                dlqEntryRepository.save(entry);
            }
        } catch (Exception e) {
            log.warn("Failed to persist DLQ operation: {}", e.getMessage());
        }
    }

    private void updateEntityStatus(FailedOperation operation, boolean success, UUID jobId) {
        try {
            String entityKey = operation.getMetadata().get("entityKey") != null ?
                    operation.getMetadata().get("entityKey").toString() : null;
            String entityType = operation.getEntityType();

            if (entityKey != null && jobId != null) {
                Optional<EntityStatus> statusOpt = entityStatusRepository.findByJobIdAndEntityTypeAndSourceIdentifier(
                        jobId, entityType, entityKey);
                if (statusOpt.isPresent()) {
                    EntityStatus status = statusOpt.get();
                    status.setStatus(success ? "RETRY_SUCCESS" : "RETRY_FAILED");
                    status.setProcessedAt(LocalDateTime.now());
                    entityStatusRepository.save(status);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update entity status after DLQ operation: {}", e.getMessage());
        }
    }

    private UUID extractJobId(FailedOperation operation) {
        Object jobIdObj = operation.getMetadata().get("jobId");
        if (jobIdObj != null) {
            try {
                return UUID.fromString(jobIdObj.toString());
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private Map<String, Object> parsePayload(String payload) {
        if (payload == null) return Map.of();
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of("raw", payload);
        }
    }

    private String generateDlqId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }

    // ========================================================================
    // CONVERSION METHODS
    // ========================================================================

    private FailedOperation toFailedOperation(DlqEntry entry) {
        return FailedOperation.builder()
                .id(entry.getId().toString())
                .operationType(entry.getOperationType())
                .entityType(entry.getEntityType())
                .entityKey(entry.getEntityKey())
                .payload(entry.getPayload())
                .errorMessage(entry.getErrorMessage())
                .errorStackTrace(entry.getErrorStackTrace())
                .attemptCount(entry.getAttemptCount())
                .firstFailure(entry.getFirstFailure() != null ?
                        entry.getFirstFailure().atZone(java.time.ZoneOffset.UTC).toInstant() : null)
                .lastAttempt(entry.getLastAttempt() != null ?
                        entry.getLastAttempt().atZone(java.time.ZoneOffset.UTC).toInstant() : null)
                .scheduledRetryTime(entry.getNextRetry() != null ?
                        entry.getNextRetry().atZone(java.time.ZoneOffset.UTC).toInstant() : null)
                .lastError(entry.getLastError())
                .status(entry.getStatus().name())
                .metadata(entry.getMetadata() != null ? entry.getMetadata() : new HashMap<>())
                .build();
    }

    private DlqEntry toDlqEntry(FailedOperation operation) {
        String entityKey = operation.getMetadata() != null ?
            String.valueOf(operation.getMetadata().get("entityKey")) : null;
        DlqEntry entry = DlqEntry.builder()
                .operationType(operation.getOperationType())
                .entityType(operation.getEntityType())
                .entityKey(entityKey)
                .payload(operation.getPayload())
                .errorMessage(operation.getErrorMessage())
                .errorStackTrace(operation.getErrorStackTrace())
                .attemptCount(operation.getAttemptCount())
                .status(parseStatus(operation.getStatus()))
                .lastError(operation.getLastError())
                .metadata(operation.getMetadata())
                .build();

        if (operation.getId() != null) {
            try {
                entry.setId(UUID.fromString(operation.getId()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (operation.getFirstFailure() != null) {
            entry.setFirstFailure(LocalDateTime.ofInstant(operation.getFirstFailure(), java.time.ZoneOffset.UTC));
        }

        if (operation.getLastAttempt() != null) {
            entry.setLastAttempt(LocalDateTime.ofInstant(operation.getLastAttempt(), java.time.ZoneOffset.UTC));
        }

        if (operation.getScheduledRetryTime() != null) {
            entry.setNextRetry(LocalDateTime.ofInstant(operation.getScheduledRetryTime(), java.time.ZoneOffset.UTC));
        }

        return entry;
    }

    private void copyToExisting(DlqEntry existing, DlqEntry updated) {
        existing.setOperationType(updated.getOperationType());
        existing.setEntityType(updated.getEntityType());
        existing.setEntityKey(updated.getEntityKey());
        existing.setPayload(updated.getPayload());
        existing.setErrorMessage(updated.getErrorMessage());
        existing.setErrorStackTrace(updated.getErrorStackTrace());
        existing.setAttemptCount(updated.getAttemptCount());
        existing.setLastAttempt(updated.getLastAttempt());
        existing.setNextRetry(updated.getNextRetry());
        existing.setStatus(updated.getStatus());
        existing.setLastError(updated.getLastError());
        existing.setMetadata(updated.getMetadata());
    }

    private DlqEntry.DlqStatus parseStatus(String status) {
        if (status == null) return DlqEntry.DlqStatus.PENDING;
        try {
            return DlqEntry.DlqStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return DlqEntry.DlqStatus.PENDING;
        }
    }

    // ========================================================================
    // DATA CLASSES
    // ========================================================================

    @Data
    @Builder
    public static class FailedOperation {
        private String id;
        private String operationType;
        private String entityType;
        private String entityKey;
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

        public void incrementSuccess() { successCount++; }
        public void incrementFailed() { failedCount++; }
        public void addError(String error) {
            if (error != null && errors.size() < 100) {
                errors.add(error);
            }
        }
    }

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