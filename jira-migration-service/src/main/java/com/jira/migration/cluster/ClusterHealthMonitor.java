package com.jira.migration.cluster;

import com.jira.migration.config.ClusterConfig;
import com.jira.migration.entity.ClusterNodeEntity;
import com.jira.migration.entity.DistributedLock;
import com.jira.migration.entity.JobClaim;
import com.jira.migration.entity.LeaderElection;
import com.jira.migration.entity.NodeState;
import com.jira.migration.repository.ClusterNodeRepository;
import com.jira.migration.repository.DistributedLockRepository;
import com.jira.migration.repository.JobClaimRepository;
import com.jira.migration.repository.LeaderElectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Monitor for cluster health.
 * Tracks node health, cluster statistics, and handles degraded states.
 */
@Slf4j
@Service
public class ClusterHealthMonitor {

    private final ClusterNodeRepository clusterNodeRepository;
    private final DistributedLockRepository lockRepository;
    private final JobClaimRepository jobClaimRepository;
    private final LeaderElectionRepository leaderElectionRepository;
    private final ClusterNodeRegistry nodeRegistry;
    private final ClusterConfig clusterConfig;

    // Health status cache
    private volatile ClusterHealth lastHealthCheck;
    private volatile Instant lastHealthCheckTime;

    @Autowired
    public ClusterHealthMonitor(ClusterNodeRepository clusterNodeRepository,
                                DistributedLockRepository lockRepository,
                                JobClaimRepository jobClaimRepository,
                                LeaderElectionRepository leaderElectionRepository,
                                ClusterNodeRegistry nodeRegistry,
                                ClusterConfig clusterConfig) {
        this.clusterNodeRepository = clusterNodeRepository;
        this.lockRepository = lockRepository;
        this.jobClaimRepository = jobClaimRepository;
        this.leaderElectionRepository = leaderElectionRepository;
        this.nodeRegistry = nodeRegistry;
        this.clusterConfig = clusterConfig;
    }

    /**
     * Get overall cluster health.
     */
    @Transactional(readOnly = true)
    public ClusterHealth getClusterHealth() {
        try {
            if (!clusterConfig.isEnabled()) {
                return ClusterHealth.builder()
                        .status(ClusterHealth.HealthStatus.HEALTHY)
                        .activeNodes(1)
                        .totalNodes(1)
                        .unhealthyNodes(List.of())
                        .warnings(List.of("Cluster coordination disabled (standalone mode)"))
                        .timestamp(System.currentTimeMillis())
                        .availabilityPercentage(100.0)
                        .build();
            }

            List<String> warnings = new ArrayList<>();
            List<String> unhealthyNodes = new ArrayList<>();

            // Count nodes by state
            List<ClusterNodeEntity> allNodes = clusterNodeRepository.findAll();
            Map<NodeState, Long> nodesByState = allNodes.stream()
                    .collect(Collectors.groupingBy(ClusterNodeEntity::getState, Collectors.counting()));

            long activeCount = nodesByState.getOrDefault(NodeState.ACTIVE, 0L);
            long totalNonTerminated = allNodes.stream()
                    .filter(n -> n.getState() != NodeState.TERMINATED)
                    .count();

            // Check for stale nodes
            Instant threshold = Instant.now().minus(clusterConfig.getHeartbeat().getTimeout());
            List<ClusterNodeEntity> staleNodes = clusterNodeRepository.findStaleActiveNodes(threshold);
            for (ClusterNodeEntity stale : staleNodes) {
                unhealthyNodes.add(stale.getNodeId());
                warnings.add("Node " + stale.getNodeId() + " has stale heartbeat");
            }

            // Check for expired locks
            long expiredLocks = lockRepository.countExpiredLocks(Instant.now());
            if (expiredLocks > 100) {
                warnings.add("High number of expired locks: " + expiredLocks);
            }

            // Check for expired job claims
            long expiredClaims = jobClaimRepository.findAllExpired(Instant.now()).size();
            if (expiredClaims > 50) {
                warnings.add("High number of expired job claims: " + expiredClaims);
            }

            // Check leader elections
            List<LeaderElection> expiredLeaders = leaderElectionRepository.findAllExpiredElections(Instant.now());
            if (!expiredLeaders.isEmpty()) {
                warnings.add("Found " + expiredLeaders.size() + " expired leader elections");
            }

            // Determine health status
            ClusterHealth.HealthStatus status;
            if (totalNonTerminated == 0) {
                status = ClusterHealth.HealthStatus.UNHEALTHY;
            } else if (!unhealthyNodes.isEmpty() || !warnings.isEmpty()) {
                status = ClusterHealth.HealthStatus.DEGRADED;
            } else if (activeCount >= totalNonTerminated) {
                status = ClusterHealth.HealthStatus.HEALTHY;
            } else {
                status = ClusterHealth.HealthStatus.DEGRADED;
            }

            // Calculate availability
            double availability = totalNonTerminated > 0
                    ? (double) activeCount / totalNonTerminated * 100.0
                    : 0.0;

            ClusterHealth health = ClusterHealth.builder()
                    .status(status)
                    .activeNodes((int) activeCount)
                    .totalNodes((int) totalNonTerminated)
                    .unhealthyNodes(unhealthyNodes)
                    .warnings(warnings)
                    .timestamp(System.currentTimeMillis())
                    .availabilityPercentage(availability)
                    .build();

            lastHealthCheck = health;
            lastHealthCheckTime = Instant.now();

            return health;

        } catch (Exception e) {
            log.error("Error checking cluster health: {}", e.getMessage(), e);
            return ClusterHealth.builder()
                    .status(ClusterHealth.HealthStatus.UNKNOWN)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * Check if operations can proceed based on cluster health.
     */
    public boolean canProceedWithOperation() {
        ClusterHealth health = getClusterHealth();
        return health.canProceedWithOperations();
    }

    /**
     * Check if operations can proceed with a specific node requirement.
     */
    public boolean canProceedWithOperation(int requiredNodes) {
        ClusterHealth health = getClusterHealth();
        return health.canProceedWithOperations() && health.getActiveNodes() >= requiredNodes;
    }

    /**
     * Get cluster statistics.
     */
    @Transactional(readOnly = true)
    public ClusterStatistics getStatistics() {
        try {
            Instant now = Instant.now();

            // Job statistics
            List<JobClaim> activeClaims = jobClaimRepository.findAllActiveClaims(now);
            int activeJobs = activeClaims.size();

            // Jobs by type
            Map<String, Integer> jobsByType = activeClaims.stream()
                    .collect(Collectors.groupingBy(
                            j -> j.getJobType() != null ? j.getJobType() : "UNKNOWN",
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

            // Nodes by state
            List<ClusterNodeEntity> allNodes = clusterNodeRepository.findAll();
            Map<String, Integer> nodesByState = allNodes.stream()
                    .collect(Collectors.groupingBy(
                            n -> n.getState().name(),
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

            // Lock statistics
            long totalLocksHeld = lockRepository.findAllActiveLocks(now).size();

            // Leader statistics
            long activeLeaders = leaderElectionRepository.countActiveLeaders(now);

            return ClusterStatistics.builder()
                    .activeJobs(activeJobs)
                    .queuedJobs(0) // Would need queue implementation
                    .completedJobsToday(0) // Would need tracking
                    .jobsByType(jobsByType)
                    .nodesByState(nodesByState)
                    .totalLocksHeld((int) totalLocksHeld)
                    .activeLeaders((int) activeLeaders)
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Error getting cluster statistics: {}", e.getMessage(), e);
            return ClusterStatistics.builder()
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * Monitor health of a specific node.
     */
    @Transactional(readOnly = true)
    public ClusterNode monitorNodeHealth(String nodeId) {
        Optional<ClusterNode> node = nodeRegistry.getNode(nodeId);
        if (node.isPresent()) {
            ClusterNode n = node.get();
            Instant threshold = Instant.now().minus(clusterConfig.getHeartbeat().getTimeout());

            if (n.getLastHeartbeat() != null && n.getLastHeartbeat().isBefore(threshold)) {
                log.warn("Node {} has stale heartbeat (last: {})", nodeId, n.getLastHeartbeat());
            }

            return n;
        }
        return null;
    }

    /**
     * Handle degraded cluster state.
     */
    @Transactional
    public void handleDegradedState() {
        ClusterHealth health = getClusterHealth();
        log.warn("Cluster is in DEGRADED state: {}", health.getSummary());

        // Mark unhealthy nodes
        for (String nodeId : health.getUnhealthyNodes()) {
            try {
                clusterNodeRepository.updateState(nodeId, NodeState.FAILED, Instant.now());
                log.info("Marked node {} as FAILED", nodeId);
            } catch (Exception e) {
                log.error("Error marking node {} as FAILED: {}", nodeId, e.getMessage());
            }
        }

        // Clean up orphaned resources
        cleanupOrphanedResources();
    }

    /**
     * Clean up orphaned resources from failed nodes.
     */
    private void cleanupOrphanedResources() {
        Instant now = Instant.now();

        try {
            // Clean up expired locks
            int cleanedLocks = lockRepository.cleanupExpiredLocks(now);
            if (cleanedLocks > 0) {
                log.info("Cleaned up {} orphaned locks", cleanedLocks);
            }
        } catch (Exception e) {
            log.error("Error cleaning up orphaned locks: {}", e.getMessage());
        }

        try {
            // Clean up expired job claims
            int cleanedClaims = jobClaimRepository.cleanupExpiredClaims(now);
            if (cleanedClaims > 0) {
                log.info("Cleaned up {} orphaned job claims", cleanedClaims);
            }
        } catch (Exception e) {
            log.error("Error cleaning up orphaned job claims: {}", e.getMessage());
        }

        try {
            // Clean up expired leader elections
            int cleanedLeaders = leaderElectionRepository.deleteExpiredElections(now);
            if (cleanedLeaders > 0) {
                log.info("Cleaned up {} orphaned leader elections", cleanedLeaders);
            }
        } catch (Exception e) {
            log.error("Error cleaning up orphaned leader elections: {}", e.getMessage());
        }
    }

    /**
     * Get last health check result.
     */
    public Optional<ClusterHealth> getLastHealthCheck() {
        return Optional.ofNullable(lastHealthCheck);
    }

    /**
     * Get last health check time.
     */
    public Optional<Instant> getLastHealthCheckTime() {
        return Optional.ofNullable(lastHealthCheckTime);
    }

    /**
     * Scheduled health check and cleanup.
     */
    @Scheduled(fixedDelayString = "${cluster.health-check.interval-seconds:30}000")
    public void scheduledHealthCheck() {
        ClusterHealth health = getClusterHealth();

        if (health.getStatus() == ClusterHealth.HealthStatus.DEGRADED ||
            health.getStatus() == ClusterHealth.HealthStatus.UNHEALTHY) {
            handleDegradedState();
        }
    }
}