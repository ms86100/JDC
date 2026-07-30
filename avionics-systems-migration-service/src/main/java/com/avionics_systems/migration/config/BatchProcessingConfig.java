package com.avionics_systems.migration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Configuration for batch processing operations.
 * All values can be configured via application.yml under the "batch" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "batch")
@Data
@Validated
public class BatchProcessingConfig {

    /**
     * Default number of records to process in a single batch.
     */
    @Min(1)
    private int defaultBatchSize = 100;

    /**
     * Maximum allowed batch size to prevent memory issues.
     */
    @Min(10)
    private int maxBatchSize = 500;

    /**
     * Number of records to read from CSV at a time for streaming processing.
     */
    @Min(10)
    private int chunkSize = 50;

    /**
     * Maximum number of retry attempts for failed operations.
     */
    @Min(0)
    @Max(10)
    private int maxRetries = 3;

    /**
     * Initial delay in milliseconds before first retry.
     */
    @Min(100)
    private long retryDelayMs = 1000;

    /**
     * Multiplier for exponential backoff between retries.
     */
    @Min(1)
    private double retryMultiplier = 2.0;

    /**
     * Enable parallel processing of independent batches.
     */
    private boolean parallelProcessingEnabled = true;

    /**
     * Maximum number of parallel threads for batch processing.
     */
    @Min(1)
    @Max(32)
    private int maxParallelThreads = 4;

    /**
     * Enable memory cleanup after each batch (System.gc() hint).
     */
    private boolean memoryCleanupEnabled = true;

    /**
     * Interval in batches after which to trigger memory cleanup.
     */
    @Min(1)
    private int memoryCleanupInterval = 10;

    /**
     * Whether to continue processing on individual batch failures.
     */
    private boolean continueOnBatchFailure = true;

    /**
     * Maximum percentage of failed records before aborting the entire job.
     * Value should be between 0 and 100.
     */
    @Min(0)
    @Max(100)
    private int maxFailurePercentage = 50;

    /**
     * Enable progress tracking and callbacks.
     */
    private boolean progressTrackingEnabled = true;

    /**
     * Size of the in-memory buffer for CSV parsing.
     */
    private int csvBufferSize = 8192;

    /**
     * Validate batch size against constraints.
     */
    public int validateBatchSize(int requestedSize) {
        if (requestedSize <= 0) {
            return defaultBatchSize;
        }
        return Math.min(requestedSize, maxBatchSize);
    }

    /**
     * Calculate the delay for a given retry attempt using exponential backoff.
     */
    public long calculateRetryDelay(int attempt) {
        if (attempt <= 0) {
            return retryDelayMs;
        }
        return (long) (retryDelayMs * Math.pow(retryMultiplier, attempt - 1));
    }
}
