package com.avionics_systems.migration.batch;

import com.avionics_systems.migration.config.RetryConfig;
import com.avionics_systems.migration.exception.RetryExhaustedException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Service for retry operations with exponential backoff and jitter.
 * Supports configurable retry policies and transient failure detection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final RetryConfig retryConfig;

    // Statistics storage
    private final Map<String, RetryStatistics> statisticsMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("RetryService initialized with config: maxAttempts={}, initialDelay={}ms, multiplier={}, jitter={}",
                retryConfig.getMaxAttempts(),
                retryConfig.getInitialDelayMs(),
                retryConfig.getMultiplier(),
                retryConfig.getJitter());
    }

    /**
     * Execute an operation with retry using default configuration.
     *
     * @param operation The operation to execute
     * @param operationName Name for tracking and logging
     * @return The result of the operation
     * @throws RetryExhaustedException if all retries are exhausted
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        return executeWithRetry(operation, operationName,
                retryConfig.getMaxAttempts(),
                retryConfig.getInitialDelayMs(),
                retryConfig.getMultiplier());
    }

    /**
     * Execute an operation with custom retry parameters.
     *
     * @param operation The operation to execute
     * @param operationName Name for tracking and logging
     * @param maxAttempts Maximum retry attempts
     * @param initialDelayMs Initial delay in milliseconds
     * @param multiplier Backoff multiplier
     * @return The result of the operation
     * @throws RetryExhaustedException if all retries are exhausted
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName,
                                    int maxAttempts, long initialDelayMs, double multiplier) {
        RetryConfig config = new RetryConfig();
        config.setMaxAttempts(maxAttempts);
        config.setInitialDelayMs(initialDelayMs);
        config.setMultiplier(multiplier);
        config.setJitter(retryConfig.getJitter());
        return executeWithRetry(operation, config);
    }

    /**
     * Execute an operation with retry using a RetryConfig.
     *
     * @param operation The operation to execute
     * @param config The retry configuration
     * @return The result of the operation
     * @throws RetryExhaustedException if all retries are exhausted
     */
    public <T> T executeWithRetry(Supplier<T> operation, RetryConfig config) {
        return executeWithRetry(operation, "anonymous", config);
    }

    /**
     * Execute an operation with retry using a named config.
     *
     * @param operation The operation to execute
     * @param operationName Name for tracking and logging
     * @param config The retry configuration
     * @return The result of the operation
     * @throws RetryExhaustedException if all retries are exhausted
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName, RetryConfig config) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < config.getMaxAttempts()) {
            attempts++;
            try {
                log.debug("Executing {} (attempt {}/{})", operationName, attempts, config.getMaxAttempts());
                T result = operation.get();

                // Success - record statistics
                recordSuccess(operationName, attempts);

                return result;

            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for {}: {}",
                        attempts, config.getMaxAttempts(), operationName, e.getMessage());

                // Check if we should continue retrying
                if (attempts >= config.getMaxAttempts()) {
                    break;
                }

                // Check if exception is retryable
                if (config.isTransientFailuresOnly() && !isTransientFailure(e)) {
                    log.error("Non-transient failure for {}, not retrying: {}", operationName, e.getMessage());
                    break;
                }

                // Calculate delay with exponential backoff and jitter
                long delay = config.calculateDelay(attempts);

                log.debug("Retrying {} after {}ms (attempt {})", operationName, delay, attempts + 1);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RetryExhaustedException(operationName, attempts, config.getMaxAttempts(), "Interrupted during retry wait");
                }
            }
        }

        // All retries exhausted
        recordFailure(operationName, attempts, lastException);
        throw new RetryExhaustedException(operationName, attempts, config.getMaxAttempts(), lastException);
    }

    /**
     * Execute an operation with retry, returning Optional on failure.
     *
     * @param operation The operation to execute
     * @param operationName Name for tracking and logging
     * @return Optional containing the result or empty if all retries failed
     */
    public <T> java.util.Optional<T> executeWithRetryOptional(Supplier<T> operation, String operationName) {
        try {
            return java.util.Optional.of(executeWithRetry(operation, operationName));
        } catch (RetryExhaustedException e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Execute a void operation with retry.
     *
     * @param operation The operation to execute
     * @param operationName Name for tracking and logging
     * @throws RetryExhaustedException if all retries are exhausted
     */
    public void executeVoidWithRetry(Runnable operation, String operationName) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, operationName);
    }

    /**
     * Check if an exception represents a transient failure that should be retried.
     */
    public boolean isTransientFailure(Exception e) {
        if (e == null) {
            return false;
        }

        // Check non-retryable list first (higher priority)
        if (retryConfig.isNonRetryableException(e.getClass())) {
            log.debug("Exception {} is explicitly non-retryable", e.getClass().getName());
            return false;
        }

        // Check retryable list
        if (retryConfig.isRetryableException(e.getClass())) {
            log.debug("Exception {} is explicitly retryable", e.getClass().getName());
            return true;
        }

        // Check by exception message patterns
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // Network-related transient failures
        if (message.contains("connection") || message.contains("timeout") ||
            message.contains("unreachable") || message.contains("refused")) {
            return true;
        }

        // Database transient failures
        if (message.contains("deadlock") || message.contains("lock wait") ||
            message.contains("temporary failure") || message.contains("too many connections")) {
            return true;
        }

        // Resource-related transient failures
        if (message.contains("busy") || message.contains("retry") ||
            message.contains("quota") || message.contains("rate limit")) {
            return true;
        }

        // Check for common transient exception types
        String className = e.getClass().getName();
        if (className.contains("Transient") || className.contains("Timeout") ||
            className.contains("Unavailable") || className.contains("Retryable")) {
            return true;
        }

        // Check if it's a nested transient failure
        if (e.getCause() != null && isTransientFailure((Exception) e.getCause())) {
            return true;
        }

        log.debug("Exception {} is not classified as transient", e.getClass().getName());
        return false;
    }

    /**
     * Get retry statistics for an operation.
     */
    public RetryStatistics getStatistics(String operationName) {
        return statisticsMap.getOrDefault(operationName, RetryStatistics.builder()
                .operationName(operationName)
                .totalAttempts(0)
                .successCount(0)
                .failureCount(0)
                .totalRetryAttempts(0)
                .maxAttemptsUsed(0)
                .build());
    }

    /**
     * Get all statistics.
     */
    public Map<String, RetryStatistics> getAllStatistics() {
        return new ConcurrentHashMap<>(statisticsMap);
    }

    /**
     * Clear statistics for an operation.
     */
    public void clearStatistics(String operationName) {
        statisticsMap.remove(operationName);
    }

    /**
     * Clear all statistics.
     */
    public void clearAllStatistics() {
        statisticsMap.clear();
    }

    /**
     * Record a successful operation.
     */
    private void recordSuccess(String operationName, int attempts) {
        if (!retryConfig.isStatisticsEnabled()) {
            return;
        }

        statisticsMap.computeIfAbsent(operationName, k -> RetryStatistics.builder()
                .operationName(k)
                .totalAttempts(0)
                .successCount(0)
                .failureCount(0)
                .totalRetryAttempts(0)
                .maxAttemptsUsed(0)
                .build())
                .recordSuccess(attempts);
    }

    /**
     * Record a failed operation.
     */
    private void recordFailure(String operationName, int attempts, Exception e) {
        if (!retryConfig.isStatisticsEnabled()) {
            return;
        }

        statisticsMap.computeIfAbsent(operationName, k -> RetryStatistics.builder()
                .operationName(k)
                .totalAttempts(0)
                .successCount(0)
                .failureCount(0)
                .totalRetryAttempts(0)
                .maxAttemptsUsed(0)
                .build())
                .recordFailure(attempts, e != null ? e.getMessage() : "Unknown error");
    }

    /**
     * Statistics holder for retry operations.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RetryStatistics {
        private String operationName;

        @lombok.Builder.Default
        private long totalAttempts = 0;

        @lombok.Builder.Default
        private long successCount = 0;

        @lombok.Builder.Default
        private long failureCount = 0;

        @lombok.Builder.Default
        private long totalRetryAttempts = 0;

        @lombok.Builder.Default
        private long maxAttemptsUsed = 0;

        @lombok.Builder.Default
        private long lastAttemptTimestamp = 0;

        @lombok.Builder.Default
        private String lastError = null;

        private transient java.util.Map<Integer, java.util.concurrent.atomic.AtomicInteger> attemptDistribution;

        public synchronized void recordSuccess(int attempts) {
            totalAttempts++;
            successCount++;
            totalRetryAttempts += (attempts - 1);
            maxAttemptsUsed = Math.max(maxAttemptsUsed, attempts);
            lastAttemptTimestamp = System.currentTimeMillis();

            // Track attempt distribution
            if (attemptDistribution == null) {
                attemptDistribution = new java.util.concurrent.ConcurrentHashMap<>();
            }
            attemptDistribution.computeIfAbsent(attempts, k -> new java.util.concurrent.atomic.AtomicInteger())
                    .incrementAndGet();
        }

        public synchronized void recordFailure(int attempts, String error) {
            totalAttempts++;
            failureCount++;
            totalRetryAttempts += (attempts - 1);
            maxAttemptsUsed = Math.max(maxAttemptsUsed, attempts);
            lastAttemptTimestamp = System.currentTimeMillis();
            lastError = error;
        }

        public double getSuccessRate() {
            return totalAttempts > 0 ? (double) successCount / totalAttempts : 0.0;
        }

        public double getAverageAttempts() {
            return totalAttempts > 0 ? (double) totalRetryAttempts / totalAttempts + 1.0 : 0.0;
        }

        public int getAttemptDistribution(int attempts) {
            return attemptDistribution != null && attemptDistribution.containsKey(attempts)
                    ? attemptDistribution.get(attempts).get() : 0;
        }
    }
}