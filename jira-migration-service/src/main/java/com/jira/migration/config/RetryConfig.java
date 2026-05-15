package com.jira.migration.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Configuration for retry operations.
 * All values can be configured via application.yml under the "retry" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "retry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Validated
public class RetryConfig {

    /**
     * Maximum number of retry attempts before giving up.
     */
    @Min(1)
    @Max(10)
    private int maxAttempts = 3;

    /**
     * Initial delay in milliseconds before first retry.
     */
    @Min(100)
    private long initialDelayMs = 1000;

    /**
     * Multiplier for exponential backoff between retries.
     * Each subsequent delay = previousDelay * multiplier
     */
    @Min(1)
    private double multiplier = 2.0;

    /**
     * Jitter factor (0.0 to 1.0) to prevent thundering herd.
     * Random delay = calculatedDelay * (1 - jitter/2 + random(0, jitter))
     */
    @Min(0)
    @Max(1)
    private double jitter = 0.3;

    /**
     * Maximum delay cap in milliseconds to prevent excessive waits.
     */
    private long maxDelayMs = 30000;

    /**
     * Enable retry for transient failures only.
     */
    private boolean transientFailuresOnly = true;

    /**
     * List of exception class names that should always be retried.
     */
    private String[] retryableExceptions = {
            "java.net.ConnectException",
            "java.net.SocketTimeoutException",
            "java.net.UnknownHostException",
            "org.springframework.dao.TransientDataAccessException",
            "org.hibernate.StaleObjectStateException",
            "org.springframework.orm.ObjectOptimisticLockingFailureException"
    };

    /**
     * List of exception class names that should never be retried.
     */
    private String[] nonRetryableExceptions = {
            "java.lang.IllegalArgumentException",
            "java.lang.NullPointerException",
            "org.springframework.dao.DataIntegrityViolationException"
    };

    /**
     * Enable retry statistics collection.
     */
    private boolean statisticsEnabled = true;

    /**
     * Calculate delay for a specific attempt number.
     */
    public long calculateDelay(int attempt) {
        if (attempt <= 0) {
            return initialDelayMs;
        }

        long delay = (long) (initialDelayMs * Math.pow(multiplier, attempt - 1));
        delay = Math.min(delay, maxDelayMs);

        // Apply jitter
        if (jitter > 0) {
            double jitterRange = delay * jitter;
            double randomOffset = Math.random() * jitterRange - (jitterRange / 2);
            delay = (long) (delay + randomOffset);
        }

        return Math.max(0, delay);
    }

    /**
     * Check if an exception is in the retryable list.
     */
    public boolean isRetryableException(Class<? extends Throwable> exceptionClass) {
        String className = exceptionClass.getName();

        for (String retryable : retryableExceptions) {
            if (className.equals(retryable) || className.startsWith(retryable)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an exception is in the non-retryable list.
     */
    public boolean isNonRetryableException(Class<? extends Throwable> exceptionClass) {
        String className = exceptionClass.getName();

        for (String nonRetryable : nonRetryableExceptions) {
            if (className.equals(nonRetryable) || className.startsWith(nonRetryable)) {
                return true;
            }
        }
        return false;
    }
}
