package com.avionics_systems.migration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for cluster coordination.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cluster")
public class ClusterConfig {

    /**
     * Unique identifier for this node in the cluster.
     */
    private String nodeId;

    /**
     * Host address for this node.
     */
    private String host;

    /**
     * Port for inter-node communication.
     */
    private int port;

    /**
     * Lock configuration.
     */
    private LockConfig lock = new LockConfig();

    /**
     * Leader election configuration.
     */
    private LeaderConfig leader = new LeaderConfig();

    /**
     * Heartbeat configuration.
     */
    private HeartbeatConfig heartbeat = new HeartbeatConfig();

    /**
     * Shutdown configuration.
     */
    private ShutdownConfig shutdown = new ShutdownConfig();

    /**
     * Whether cluster coordination is enabled.
     */
    private boolean enabled = true;

    /**
     * Configuration for distributed locking.
     */
    @Data
    public static class LockConfig {
        /**
         * Lock implementation type: DATABASE or REDIS.
         */
        private String type = "DATABASE";

        /**
         * Default TTL for locks.
         */
        private int defaultTtlMinutes = 5;

        /**
         * Wait timeout for acquiring locks.
         */
        private int waitTimeoutSeconds = 30;

        /**
         * Cleanup interval for expired locks.
         */
        private int cleanupIntervalSeconds = 60;

        /**
         * Maximum retry attempts for lock acquisition.
         */
        private int maxRetries = 3;

        /**
         * Delay between retry attempts.
         */
        private int retryDelayMillis = 100;

        /**
         * Get default TTL as Duration.
         */
        public Duration getDefaultTtl() {
            return Duration.ofMinutes(defaultTtlMinutes);
        }

        /**
         * Get wait timeout as Duration.
         */
        public Duration getWaitTimeout() {
            return Duration.ofSeconds(waitTimeoutSeconds);
        }
    }

    /**
     * Configuration for leader election.
     */
    @Data
    public static class LeaderConfig {
        /**
         * Whether leader election is enabled.
         */
        private boolean enabled = true;

        /**
         * Duration of leader lease.
         */
        private int leaseDurationMinutes = 1;

        /**
         * Interval for leader heartbeat renewal.
         */
        private int renewalIntervalSeconds = 30;

        /**
         * Timeout for leader heartbeat.
         */
        private int heartbeatTimeoutSeconds = 60;

        /**
         * Minimum number of votes required to become leader.
         */
        private int minVotes = 1;

        /**
         * Get lease duration as Duration.
         */
        public Duration getLeaseDuration() {
            return Duration.ofMinutes(leaseDurationMinutes);
        }

        /**
         * Get renewal interval as Duration.
         */
        public Duration getRenewalInterval() {
            return Duration.ofSeconds(renewalIntervalSeconds);
        }

        /**
         * Get heartbeat timeout as Duration.
         */
        public Duration getHeartbeatTimeout() {
            return Duration.ofSeconds(heartbeatTimeoutSeconds);
        }
    }

    /**
     * Configuration for node heartbeat.
     */
    @Data
    public static class HeartbeatConfig {
        /**
         * Interval between heartbeats.
         */
        private int intervalSeconds = 10;

        /**
         * Timeout after which a node is considered stale.
         */
        private int timeoutMinutes = 1;

        /**
         * Cleanup interval for stale nodes.
         */
        private int cleanupIntervalSeconds = 60;

        /**
         * Get interval as Duration.
         */
        public Duration getInterval() {
            return Duration.ofSeconds(intervalSeconds);
        }

        /**
         * Get timeout as Duration.
         */
        public Duration getTimeout() {
            return Duration.ofMinutes(timeoutMinutes);
        }
    }

    /**
     * Configuration for graceful shutdown.
     */
    @Data
    public static class ShutdownConfig {
        /**
         * Grace period before force shutdown.
         */
        private int gracefulTimeoutSeconds = 30;

        /**
         * Whether to force shutdown if not complete within timeout.
         */
        private boolean forceIfNotComplete = false;

        /**
         * Get graceful timeout as Duration.
         */
        public Duration getGracefulTimeout() {
            return Duration.ofSeconds(gracefulTimeoutSeconds);
        }
    }
}