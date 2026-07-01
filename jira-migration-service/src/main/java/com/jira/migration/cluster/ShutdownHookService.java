package com.jira.migration.cluster;

import com.jira.migration.config.ClusterConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to handle graceful shutdown of cluster coordination.
 * Ensures all locks are released and leadership is resigned on shutdown.
 */
@Slf4j
@Service
public class ShutdownHookService {

    private final DistributedLockService lockService;
    private final ClusterNodeRegistry nodeRegistry;
    private final LeaderElectionServiceImpl leaderElectionService;
    private final ClusterConfig clusterConfig;

    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    @Autowired
    public ShutdownHookService(DistributedLockService lockService,
                                ClusterNodeRegistry nodeRegistry,
                                LeaderElectionServiceImpl leaderElectionService,
                                ClusterConfig clusterConfig) {
        this.lockService = lockService;
        this.nodeRegistry = nodeRegistry;
        this.leaderElectionService = leaderElectionService;
        this.clusterConfig = clusterConfig;
    }

    /**
     * Called when the application is shutting down.
     * Ensures graceful cleanup of cluster coordination resources.
     */
    @PreDestroy
    public void onShutdown() {
        if (!shutdownInProgress.compareAndSet(false, true)) {
            log.info("Shutdown already in progress");
            return;
        }

        log.info("Initiating graceful shutdown of cluster coordination...");
        long startTime = System.currentTimeMillis();

        try {
            // Get the graceful timeout
            Duration timeout = clusterConfig.getShutdown().getGracefulTimeout();
            Instant deadline = Instant.now().plus(timeout);

            // 1. Signal leadership resignation
            resignAllLeadership();

            // 2. Release all locks held by this node
            releaseAllLocks();

            // 3. Complete in-progress operations (with timeout)
            completeInProgressOperations(timeout);

            // 4. Deregister from cluster
            deregisterNode();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Graceful shutdown completed in {} ms", elapsed);

        } catch (Exception e) {
            log.error("Error during graceful shutdown: {}", e.getMessage(), e);

            if (clusterConfig.getShutdown().isForceIfNotComplete()) {
                log.warn("Force shutdown requested after graceful period exceeded");
                forceShutdown();
            }
        } finally {
            shutdownLatch.countDown();
        }
    }

    /**
     * Wait for shutdown to complete.
     */
    public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
        return shutdownLatch.await(timeout, unit);
    }

    /**
     * Check if shutdown is in progress.
     */
    public boolean isShutdownInProgress() {
        return shutdownInProgress.get();
    }

    /**
     * Resign from all leadership positions.
     */
    private void resignAllLeadership() {
        log.info("Resigning from all leadership positions...");

        try {
            var leadershipGroups = leaderElectionService.getLeadershipGroups();
            for (String group : leadershipGroups) {
                if (leaderElectionService.isLeader(group)) {
                    try {
                        leaderElectionService.resign(group);
                        log.info("Resigned from leadership group: {}", group);
                    } catch (Exception e) {
                        log.error("Error resigning from leadership group {}: {}", group, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting leadership groups: {}", e.getMessage());
        }
    }

    /**
     * Release all locks held by this node.
     */
    private void releaseAllLocks() {
        log.info("Releasing all locks held by this node...");

        try {
            lockService.releaseAllLocks();
            log.info("All locks released");
        } catch (Exception e) {
            log.error("Error releasing locks: {}", e.getMessage());
        }
    }

    /**
     * Wait for in-progress operations to complete.
     */
    private void completeInProgressOperations(Duration maxWait) {
        log.info("Waiting for in-progress operations to complete (max {} seconds)...", maxWait.getSeconds());

        // Check for active jobs
        ClusterNode thisNode = nodeRegistry.getThisNode();
        int activeJobs = thisNode != null ? thisNode.getCurrentJobs() : 0;

        if (activeJobs > 0) {
            log.info("Waiting for {} active jobs to complete...", activeJobs);

            // In a real implementation, this would coordinate with the job processor
            // to wait for jobs to complete or be reassigned

            // For now, we just log and proceed
            try {
                Thread.sleep(Math.min(maxWait.toMillis(), 5000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for operations to complete");
            }
        }
    }

    /**
     * Deregister this node from the cluster.
     */
    private void deregisterNode() {
        log.info("Deregistering this node from the cluster...");

        try {
            nodeRegistry.deregisterNode();
            log.info("Node deregistered successfully");
        } catch (Exception e) {
            log.error("Error deregistering node: {}", e.getMessage());
        }
    }

    /**
     * Force shutdown (called when graceful shutdown times out).
     */
    private void forceShutdown() {
        log.warn("Force shutdown - releasing all resources immediately...");

        try {
            // Force release all locks (even if we might not own them in edge cases)
            lockService.releaseAllLocks();
        } catch (Exception e) {
            log.error("Error during force release: {}", e.getMessage());
        }

        try {
            // Force deregister
            nodeRegistry.deregisterNode();
        } catch (Exception e) {
            log.error("Error during force deregister: {}", e.getMessage());
        }
    }

    /**
     * Register a shutdown callback.
     */
    public void registerShutdownCallback(Runnable callback) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                callback.run();
            } catch (Exception e) {
                log.error("Error in shutdown callback: {}", e.getMessage());
            }
        }));
    }
}