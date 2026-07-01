package com.jira.migration.coordinator;

import com.jira.migration.cluster.*;
import com.jira.migration.config.ClusterConfig;
import com.jira.migration.entity.JobClaim;
import com.jira.migration.repository.JobClaimRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Coordinator for distributed import job execution.
 * Manages job claiming, lock coordination, and failure recovery.
 */
@Slf4j
@Service
public class ImportJobCoordinator {

    private final DistributedLockService lockService;
    private final ClusterNodeRegistry nodeRegistry;
    private final JobClaimRepository jobClaimRepository;
    private final ClusterConfig clusterConfig;

    // Resource name patterns
    private static final String JOB_LOCK_PREFIX = "job:lock:";
    private static final String JOB_CLAIM_PREFIX = "job:claim:";

    @Autowired
    public ImportJobCoordinator(DistributedLockService lockService,
                                 ClusterNodeRegistry nodeRegistry,
                                 JobClaimRepository jobClaimRepository,
                                 ClusterConfig clusterConfig) {
        this.lockService = lockService;
        this.nodeRegistry = nodeRegistry;
        this.jobClaimRepository = jobClaimRepository;
        this.clusterConfig = clusterConfig;
    }

    /**
     * Coordinate distributed execution of an import job.
     *
     * @param jobId The job ID to coordinate
     * @param job   The job configuration
     * @return Result of the coordination
     */
    public ImportCoordinatorResult coordinateJobExecution(String jobId, ImportJob job) {
        log.info(" Coordinating job execution for jobId: {}", jobId);

        // Step 1: Try to acquire job lock
        String lockResource = JOB_LOCK_PREFIX + jobId;
        Duration lockTtl = clusterConfig.getLock().getDefaultTtl();
        Optional<LockHandle> lockHandle = lockService.tryAcquireLock(lockResource, lockTtl);

        if (lockHandle.isEmpty()) {
            // Lock is held by another node
            Optional<LockInfo> lockInfo = lockService.getLockInfo(lockResource);
            String holderNode = lockInfo.map(LockInfo::getNodeId).orElse("unknown");
            log.info("Job {} is locked by node {}", jobId, holderNode);
            return ImportCoordinatorResult.alreadyProcessing(holderNode);
        }

        LockHandle acquiredLock = lockHandle.get();
        String thisNodeId = nodeRegistry.getThisNode().getNodeId();

        // Step 2: Create job claim
        Optional<JobClaim> claimOpt = createJobClaim(jobId, acquiredLock.getLockId(), job);

        if (claimOpt.isEmpty()) {
            // Failed to create claim (already exists)
            lockService.releaseLock(acquiredLock);
            return ImportCoordinatorResult.failure("Failed to create job claim - job may already be claimed");
        }

        JobClaim claim = claimOpt.get();

        // Step 3: Update job count for this node
        nodeRegistry.incrementJobCount();

        log.info("Job {} coordinated successfully on node {} (lockId: {})",
                jobId, thisNodeId, acquiredLock.getLockId());

        return ImportCoordinatorResult.success(thisNodeId, acquiredLock, claim);
    }

    /**
     * Claim a job for processing by this node.
     *
     * @param jobId The job ID to claim
     * @return Optional containing the job claim if successful
     */
    public Optional<JobClaim> claimJob(String jobId) {
        String lockResource = JOB_LOCK_PREFIX + jobId;
        Duration lockTtl = clusterConfig.getLock().getDefaultTtl();
        Optional<LockHandle> lockHandle = lockService.tryAcquireLock(lockResource, lockTtl);

        if (lockHandle.isEmpty()) {
            return Optional.empty();
        }

        return createJobClaim(jobId, lockHandle.get().getLockId(), null);
    }

    /**
     * Release a job claim.
     *
     * @param jobId The job ID to release
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseJobClaim(String jobId) {
        log.info("Releasing job claim for jobId: {}", jobId);

        String lockResource = JOB_LOCK_PREFIX + jobId;
        Optional<LockInfo> lockInfo = lockService.getLockInfo(lockResource);

        if (lockInfo.isPresent()) {
            LockInfo info = lockInfo.get();
            String thisNodeId = nodeRegistry.getThisNode().getNodeId();

            // Verify this node owns the lock
            if (thisNodeId.equals(info.getNodeId())) {
                // Release claim
                jobClaimRepository.releaseClaim(jobId, info.getLockId());

                // Release lock
                lockService.releaseLock(lockResource, info.getLockId());

                // Decrement job count
                nodeRegistry.decrementJobCount();

                log.info("Released job claim for jobId: {}", jobId);
            } else {
                log.warn("Cannot release job claim for {} - not owned by this node", jobId);
            }
        }
    }

    /**
     * Check if a job is currently being processed.
     *
     * @param jobId The job ID to check
     * @return true if the job is being processed
     */
    public boolean isJobBeingProcessed(String jobId) {
        String lockResource = JOB_LOCK_PREFIX + jobId;
        return lockService.isLocked(lockResource);
    }

    /**
     * Get job processing status across the cluster.
     *
     * @param jobId The job ID to check
     * @return Map of node ID to processing info
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, JobProcessingInfo> getClusterJobStatus(String jobId) {
        Map<String, JobProcessingInfo> statusMap = new HashMap<>();

        // Check lock status
        String lockResource = JOB_LOCK_PREFIX + jobId;
        Optional<LockInfo> lockInfo = lockService.getLockInfo(lockResource);

        if (lockInfo.isPresent()) {
            LockInfo info = lockInfo.get();
            ClusterNode node = nodeRegistry.getNode(info.getNodeId()).orElse(null);

            statusMap.put(info.getNodeId(), JobProcessingInfo.builder()
                    .jobId(jobId)
                    .inProgress(true)
                    .processingNode(info.getNodeId())
                    .lastUpdate(info.getExpiresAt())
                    .build());
        }

        // Check claim status
        Optional<JobClaim> claim = jobClaimRepository.findByJobId(jobId);
        if (claim.isPresent() && !claim.get().isExpired()) {
            JobClaim entity = claim.get();
            JobProcessingInfo existing = statusMap.get(entity.getNodeId());
            if (existing != null) {
                existing.setProgressPercentage(entity.getProgressPercentage());
                existing.setJobType(entity.getJobType());
            } else {
                statusMap.put(entity.getNodeId(), JobProcessingInfo.builder()
                        .jobId(jobId)
                        .inProgress(true)
                        .processingNode(entity.getNodeId())
                        .progressPercentage(entity.getProgressPercentage())
                        .lastUpdate(entity.getLastUpdate())
                        .jobType(entity.getJobType())
                        .build());
            }
        }

        return statusMap;
    }

    /**
     * Handle node failure during job processing.
     *
     * @param nodeId The failed node ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNodeFailure(String nodeId) {
        log.warn("Handling failure of node: {}", nodeId);

        // Find all claims by this node
        List<JobClaim> claims = jobClaimRepository.findActiveClaimsByNode(nodeId, Instant.now());
        log.info("Found {} active claims for failed node {}", claims.size(), nodeId);

        // Claims will be cleaned up by TTL expiration
        // Jobs can be reassigned if needed

        // Clean up any locks held by this node (should already be expired)
        // This is handled by the lock cleanup mechanism
    }

    /**
     * Reassign jobs from a failed node to available nodes.
     *
     * @param nodeId The failed node ID
     * @return List of job IDs that were reassigned
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> reassignJobsFromNode(String nodeId) {
        log.info("Reassigning jobs from failed node: {}", nodeId);

        List<String> reassignedJobs = new ArrayList<>();

        // Find all claims by this node
        List<JobClaim> claims = jobClaimRepository.findActiveClaimsByNode(nodeId, Instant.now());

        for (JobClaim claim : claims) {
            String jobId = claim.getJobId();

            // Try to acquire the lock
            String lockResource = JOB_LOCK_PREFIX + jobId;
            Optional<LockHandle> lockHandle = lockService.tryAcquireLock(lockResource,
                    clusterConfig.getLock().getDefaultTtl());

            if (lockHandle.isPresent()) {
                // Update claim to new node (or delete and let it be re-claimed)
                jobClaimRepository.releaseClaim(jobId, claim.getLockId());
                reassignedJobs.add(jobId);
                log.info("Job {} marked for reassignment (previous owner: {})", jobId, nodeId);
            }
        }

        return reassignedJobs;
    }

    /**
     * Update progress for a job claim.
     *
     * @param jobId         The job ID
     * @param progress      Progress percentage (0-100)
     * @param nodeId        The node updating progress
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobProgress(String jobId, double progress, String nodeId) {
        jobClaimRepository.updateProgress(jobId, nodeId, progress, Instant.now());
    }

    /**
     * Extend a job claim's TTL.
     *
     * @param jobId     The job ID
     * @param nodeId    The node ID
     * @param lockId    The lock ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean extendJobClaim(String jobId, String nodeId, String lockId) {
        Instant newExpiry = Instant.now().plus(clusterConfig.getLock().getDefaultTtl());
        int updated = jobClaimRepository.extendClaim(jobId, lockId, nodeId, newExpiry, Instant.now());

        if (updated > 0) {
            log.debug("Extended job claim for jobId: {}", jobId);
            return true;
        }
        return false;
    }

    /**
     * Create a job claim entry.
     */
    private Optional<JobClaim> createJobClaim(String jobId, String lockId, ImportJob job) {
        String thisNodeId = nodeRegistry.getThisNode().getNodeId();
        String thisOwnerId = nodeRegistry.getThisNode().getNodeId();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(clusterConfig.getLock().getDefaultTtl());

        try {
            // Check if claim already exists
            Optional<JobClaim> existing = jobClaimRepository.findByJobId(jobId);
            if (existing.isPresent() && !existing.get().isExpired()) {
                log.debug("Job claim already exists for jobId: {}", jobId);
                return Optional.empty();
            }

            // Delete any expired claim
            if (existing.isPresent()) {
                jobClaimRepository.delete(existing.get());
            }

            // Create new claim
            JobClaim newClaim = JobClaim.builder()
                    .jobId(jobId)
                    .lockId(lockId)
                    .nodeId(thisNodeId)
                    .ownerId(thisOwnerId)
                    .claimedAt(now)
                    .expiresAt(expiresAt)
                    .jobType(job != null ? job.getJobType() : "IMPORT")
                    .priority(0)
                    .progressPercentage(0.0)
                    .lastUpdate(now)
                    .build();

            jobClaimRepository.save(newClaim);

            return Optional.of(JobClaim.builder()
                    .jobId(jobId)
                    .lockId(lockId)
                    .nodeId(thisNodeId)
                    .ownerId(thisOwnerId)
                    .claimedAt(now)
                    .expiresAt(expiresAt)
                    .jobType(job != null ? job.getJobType() : "IMPORT")
                    .priority(0)
                    .progressPercentage(0.0)
                    .lastUpdate(now)
                    .build());

        } catch (Exception e) {
            log.error("Error creating job claim for jobId {}: {}", jobId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Scheduled task to clean up expired job claims.
     */
    @Scheduled(fixedDelayString = "${cluster.lock.cleanup-interval-seconds:60}000")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupExpiredClaims() {
        try {
            int cleaned = jobClaimRepository.cleanupExpiredClaims(Instant.now());
            if (cleaned > 0) {
                log.info("Cleaned up {} expired job claims", cleaned);
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired job claims: {}", e.getMessage(), e);
        }
    }

    /**
     * Represents an import job for coordination.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ImportJob {
        private String jobId;
        private String jobType;
        private String source;
        private String target;
        private Map<String, Object> options;
    }
}