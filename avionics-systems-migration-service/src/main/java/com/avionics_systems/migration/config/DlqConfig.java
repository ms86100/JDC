package com.avionics_systems.migration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration for Dead Letter Queue operations.
 * All values can be configured via application.yml under the "dlq" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "dlq")
@Data
@Validated
public class DlqConfig {

    /**
     * Enable/disable the DLQ functionality.
     */
    private boolean enabled = true;

    /**
     * Number of days to retain failed operations in the DLQ.
     */
    @Min(1)
    private int retentionDays = 7;

    /**
     * Enable automatic retry of failed operations.
     */
    private boolean autoRetry = true;

    /**
     * Hours to wait before auto-retrying a failed operation.
     */
    @Min(1)
    private int autoRetryDelayHours = 24;

    /**
     * Maximum number of retry attempts for auto-retry.
     */
    @Min(1)
    private int maxAutoRetryAttempts = 3;

    /**
     * Enable DLQ metrics and monitoring.
     */
    private boolean metricsEnabled = true;

    /**
     * Maximum size of the DLQ before oldest entries are discarded.
     */
    @Min(100)
    private int maxQueueSize = 10000;

    /**
     * Enable email notification when items enter DLQ.
     */
    private boolean emailNotificationEnabled = false;

    /**
     * Email addresses to notify (comma-separated).
     */
    private String notificationEmails;

    /**
     * Batch size for retry operations.
     */
    @Min(1)
    private int retryBatchSize = 100;

    /**
     * Enable dead letter queue cleanup task.
     */
    private boolean cleanupEnabled = true;

    /**
     * Cron expression for DLQ cleanup schedule.
     * Default: every day at 2 AM
     */
    private String cleanupCron = "0 0 2 * * *";

    /**
     * Check if DLQ is enabled and operational.
     */
    public boolean isOperational() {
        return enabled;
    }

    /**
     * Get retention duration in milliseconds.
     */
    public long getRetentionDurationMs() {
        return (long) retentionDays * 24 * 60 * 60 * 1000;
    }
}
