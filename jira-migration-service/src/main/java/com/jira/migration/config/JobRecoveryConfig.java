package com.jira.migration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration for job recovery operations.
 * All values can be configured via application.yml under the "job.recovery" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "job.recovery")
@Data
@Validated
public class JobRecoveryConfig {

    /**
     * Enable job recovery on application startup.
     */
    private boolean enabled = true;

    /**
     * Age in hours after which stale jobs are considered for cleanup.
     */
    @Min(1)
    private int cleanupStaleAfterHours = 24;

    /**
     * Check for interrupted jobs on startup.
     */
    private boolean checkInterruptedOnStartup = true;

    /**
     * Age in hours after which stuck jobs are force-completed.
     */
    @Min(1)
    private int forceCompleteAfterHours = 48;

    /**
     * Enable automatic recovery of interrupted jobs.
     */
    private boolean autoRecover = true;

    /**
     * Maximum number of jobs to recover in a single run.
     */
    @Min(1)
    private int maxRecoveriesPerRun = 100;

    /**
     * Enable recovery of failed batches.
     */
    private boolean recoverFailedBatches = true;

    /**
     * Enable cleanup of orphaned resources.
     */
    private boolean cleanupOrphanedResources = true;

    /**
     * Cron expression for scheduled recovery check.
     * Default: every 15 minutes
     */
    private String recoveryCheckCron = "0 */15 * * * *";

    /**
     * Cron expression for scheduled stale job cleanup.
     * Default: every hour
     */
    private String cleanupCron = "0 0 * * * *";

    /**
     * Enable recovery status persistence.
     */
    private boolean persistRecoveryStatus = true;

    /**
     * Check if recovery is enabled and should run.
     */
    public boolean shouldRunRecovery() {
        return enabled && autoRecover;
    }

    /**
     * Get stale job threshold in milliseconds.
     */
    public long getStaleThresholdMs() {
        return (long) cleanupStaleAfterHours * 60 * 60 * 1000;
    }

    /**
     * Get force complete threshold in milliseconds.
     */
    public long getForceCompleteThresholdMs() {
        return (long) forceCompleteAfterHours * 60 * 60 * 1000;
    }
}
