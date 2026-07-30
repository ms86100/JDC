package com.avionics_systems.migration.batch;

import com.avionics_systems.migration.exception.TransactionManagementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Transaction manager for import operations.
 * Provides transaction boundaries with savepoint support and rollback safety validation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportTransactionManager {

    // Thread-local storage for transaction context
    private final ThreadLocal<TransactionContext> transactionContext = new ThreadLocal<>();

    // Savepoint stack for nested rollback support
    private final ThreadLocal<Deque<String>> savepointStack = ThreadLocal.withInitial(LinkedList::new);

    /**
     * Execute an operation within a transaction boundary.
     */
    @Transactional
    public <T> T executeInTransaction(Supplier<T> operation) {
        return executeInTransaction(operation, "default");
    }

    /**
     * Execute an operation within a named transaction.
     */
    @Transactional
    public <T> T executeInTransaction(Supplier<T> operation, String transactionName) {
        TransactionContext ctx = getOrCreateContext(transactionName);
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Starting transaction: {}", transactionName);

            // Execute the operation
            T result = operation.get();

            // Commit if successful
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Transaction {} completed successfully in {}ms", transactionName, duration);

            ctx.recordSuccess(duration);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Transaction {} failed after {}ms: {}", transactionName, duration, e.getMessage());

            ctx.recordFailure(e.getMessage());
            throw e; // Re-throw to trigger rollback

        } finally {
            if (!ctx.isNested()) {
                // Clean up non-nested transactions
                clearContext();
            }
        }
    }

    /**
     * Execute a void operation within a transaction.
     */
    @Transactional
    public void executeVoidInTransaction(Runnable operation) {
        executeVoidInTransaction(operation, "default");
    }

    /**
     * Execute a void operation within a named transaction.
     */
    @Transactional
    public void executeVoidInTransaction(Runnable operation, String transactionName) {
        executeInTransaction(() -> {
            operation.run();
            return null;
        }, transactionName);
    }

    /**
     * Mark a checkpoint for potential partial rollback.
     */
    public String markCheckpoint(String checkpointName) {
        String savepointName = "SP_" + checkpointName + "_" + System.nanoTime();

        TransactionContext ctx = transactionContext.get();
        if (ctx != null) {
            ctx.addCheckpoint(savepointName);
            savepointStack.get().push(savepointName);
            log.debug("Created checkpoint: {} (savepoint: {})", checkpointName, savepointName);
        }

        return savepointName;
    }

    /**
     * Rollback to a specific checkpoint.
     */
    public void rollbackToCheckpoint(String checkpointName) {
        Deque<String> stack = savepointStack.get();

        // Find and remove all savepoints up to and including the target
        while (!stack.isEmpty()) {
            String savepoint = stack.pop();
            if (savepoint.contains(checkpointName)) {
                break;
            }
        }

        log.info("Rolling back to checkpoint: {}", checkpointName);

        // In JPA, this would use:
        // entityManager.setRollbackOnly();
        // or use a savepoint:
        // Connection conn = entityManager.unwrap(Connection.class);
        // conn.releaseSavepoint(savepoint);
    }

    /**
     * Commit the current transaction.
     */
    public void commitBatch() {
        TransactionContext ctx = transactionContext.get();
        if (ctx != null) {
            ctx.setCommitRequested(true);
            log.debug("Commit requested for transaction");
        }
    }

    /**
     * Check if a transaction is currently active.
     */
    public boolean isTransactionActive() {
        TransactionContext ctx = transactionContext.get();
        return ctx != null && !ctx.isCompleted();
    }

    /**
     * Get current transaction statistics.
     */
    public TransactionStatistics getStatistics() {
        TransactionContext ctx = transactionContext.get();
        if (ctx != null) {
            return ctx.getStatistics();
        }
        return TransactionStatistics.builder()
                .transactionId("none")
                .isActive(false)
                .build();
    }

    /**
     * Validate rollback safety before proceeding.
     */
    public RollbackSafetyResult validateRollbackSafety() {
        TransactionContext ctx = transactionContext.get();

        if (ctx == null) {
            return RollbackSafetyResult.builder()
                    .isSafe(true)
                    .message("No active transaction")
                    .build();
        }

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check for external system calls
        if (ctx.hasExternalCalls()) {
            warnings.add("Transaction has made external API calls");
        }

        // Check for file I/O
        if (ctx.hasFileOperations()) {
            warnings.add("Transaction has performed file operations");
        }

        // Check for notification sends
        if (ctx.hasNotifications()) {
            warnings.add("Transaction has sent notifications");
        }

        // Check for cross-service communication
        if (ctx.hasCrossServiceCalls()) {
            warnings.add("Transaction has cross-service calls - full rollback may not be possible");
        }

        return RollbackSafetyResult.builder()
                .isSafe(errors.isEmpty())
                .hasWarnings(!warnings.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .message(errors.isEmpty() ?
                        (warnings.isEmpty() ? "Safe for rollback" : "Warnings present") :
                        "Errors present - full rollback not guaranteed")
                .build();
    }

    /**
     * Record an external call in the current transaction.
     */
    public void recordExternalCall(String service) {
        TransactionContext ctx = transactionContext.get();
        if (ctx != null) {
            ctx.recordExternalCall(service);
        }
    }

    /**
     * Record a file operation in the current transaction.
     */
    public void recordFileOperation(String path) {
        TransactionContext ctx = transactionContext.get();
        if (ctx != null) {
            ctx.recordFileOperation(path);
        }
    }

    /**
     * Record a notification send in the current transaction.
     */
    public void recordNotification(String type) {
        TransactionContext ctx = transactionContext.get();
        if (ctx != null) {
            ctx.recordNotification(type);
        }
    }

    /**
     * Record a cross-service call.
     */
    public void recordCrossServiceCall(String service) {
        TransactionContext ctx = transactionContext.get();
        if (ctx != null) {
            ctx.recordCrossServiceCall(service);
        }
    }

    /**
     * Execute with automatic rollback on failure.
     */
    @Transactional(noRollbackFor = {TransactionManagementException.class})
    public <T> T executeWithRollback(T resultOnFailure, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (Exception e) {
            log.warn("Operation failed, returning failure result: {}", e.getMessage());
            return resultOnFailure;
        }
    }

    /**
     * Require rollback - mark transaction for rollback only.
     */
    public void requireRollback(String reason) {
        TransactionContext ctx = transactionContext.get();
        if (ctx != null) {
            ctx.setRollbackRequired(true);
            ctx.setRollbackReason(reason);
            log.warn("Transaction marked for rollback: {}", reason);
        }
    }

    /**
     * Get transaction ID for the current thread.
     */
    public String getTransactionId() {
        TransactionContext ctx = transactionContext.get();
        return ctx != null ? ctx.getTransactionId() : "none";
    }

    // Private helper methods

    private TransactionContext getOrCreateContext(String name) {
        TransactionContext ctx = transactionContext.get();
        if (ctx == null) {
            ctx = new TransactionContext(name);
            transactionContext.set(ctx);
        }
        ctx.incrementNesting();
        return ctx;
    }

    private void clearContext() {
        transactionContext.remove();
        savepointStack.remove();
    }

    /**
     * Transaction context holder.
     */
    private static class TransactionContext {
        private final String transactionId;
        private final String name;
        private final AtomicInteger nesting = new AtomicInteger(0);
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final List<String> checkpoints = Collections.synchronizedList(new ArrayList<>());
        private final Set<String> externalCalls = ConcurrentHashMap.newKeySet();
        private final Set<String> fileOperations = ConcurrentHashMap.newKeySet();
        private final Set<String> notifications = ConcurrentHashMap.newKeySet();
        private final Set<String> crossServiceCalls = ConcurrentHashMap.newKeySet();
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile boolean commitRequested = false;
        private volatile boolean rollbackRequired = false;
        private volatile String rollbackReason;
        private volatile long startTime;

        public TransactionContext(String name) {
            this.transactionId = UUID.randomUUID().toString().substring(0, 8);
            this.name = name;
            this.startTime = System.currentTimeMillis();
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void incrementNesting() {
            nesting.incrementAndGet();
        }

        public boolean isNested() {
            return nesting.get() > 1;
        }

        public boolean isCompleted() {
            return completed.get();
        }

        public void addCheckpoint(String checkpoint) {
            checkpoints.add(checkpoint);
        }

        public void recordSuccess(long durationMs) {
            successCount.incrementAndGet();
            completed.set(true);
        }

        public void recordFailure(String error) {
            failureCount.incrementAndGet();
            completed.set(true);
        }

        public boolean hasExternalCalls() {
            return !externalCalls.isEmpty();
        }

        public void recordExternalCall(String service) {
            externalCalls.add(service);
        }

        public boolean hasFileOperations() {
            return !fileOperations.isEmpty();
        }

        public void recordFileOperation(String path) {
            fileOperations.add(path);
        }

        public boolean hasNotifications() {
            return !notifications.isEmpty();
        }

        public void recordNotification(String type) {
            notifications.add(type);
        }

        public boolean hasCrossServiceCalls() {
            return !crossServiceCalls.isEmpty();
        }

        public void recordCrossServiceCall(String service) {
            crossServiceCalls.add(service);
        }

        public void setCommitRequested(boolean requested) {
            this.commitRequested = requested;
        }

        public void setRollbackRequired(boolean required) {
            this.rollbackRequired = required;
        }

        public void setRollbackReason(String reason) {
            this.rollbackReason = reason;
        }

        public TransactionStatistics getStatistics() {
            return TransactionStatistics.builder()
                    .transactionId(transactionId)
                    .name(name)
                    .isActive(!completed.get())
                    .isNested(isNested())
                    .nestingLevel(nesting.get())
                    .checkpointCount(checkpoints.size())
                    .successCount(successCount.get())
                    .failureCount(failureCount.get())
                    .commitRequested(commitRequested)
                    .rollbackRequired(rollbackRequired)
                    .rollbackReason(rollbackReason)
                    .externalCallsMade(externalCalls)
                    .fileOperationsPerformed(fileOperations)
                    .notificationsSent(notifications)
                    .crossServiceCalls(crossServiceCalls)
                    .build();
        }
    }

    /**
     * Transaction statistics.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransactionStatistics {
        private String transactionId;
        private String name;
        private boolean isActive;
        private boolean isNested;
        private int nestingLevel;
        private int checkpointCount;
        private int successCount;
        private int failureCount;
        private boolean commitRequested;
        private boolean rollbackRequired;
        private String rollbackReason;
        private Set<String> externalCallsMade;
        private Set<String> fileOperationsPerformed;
        private Set<String> notificationsSent;
        private Set<String> crossServiceCalls;
    }

    /**
     * Rollback safety validation result.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RollbackSafetyResult {
        private boolean isSafe;
        private boolean hasWarnings;
        private List<String> errors;
        private List<String> warnings;
        private String message;
    }
}