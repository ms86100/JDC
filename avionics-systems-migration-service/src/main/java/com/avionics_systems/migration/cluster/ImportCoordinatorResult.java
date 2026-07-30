package com.avionics_systems.migration.cluster;

import com.avionics_systems.migration.entity.JobClaim;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result of coordinating an import job execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCoordinatorResult {

    /**
     * Whether the job can proceed.
     */
    private boolean canProceed;

    /**
     * Reason if the job cannot proceed.
     */
    private String reason;

    /**
     * Node that will process the job.
     */
    private String processingNode;

    /**
     * Whether the lock was acquired.
     */
    private boolean acquiredLock;

    /**
     * Lock handle if acquired.
     */
    private LockHandle lockHandle;

    /**
     * Job claim if created.
     */
    private JobClaim jobClaim;

    /**
     * Timestamp of the decision.
     */
    private Instant timestamp;

    /**
     * Create a successful result.
     */
    public static ImportCoordinatorResult success(String nodeId, LockHandle lockHandle, JobClaim claim) {
        return ImportCoordinatorResult.builder()
                .canProceed(true)
                .reason("Lock acquired successfully")
                .processingNode(nodeId)
                .acquiredLock(true)
                .lockHandle(lockHandle)
                .jobClaim(claim)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a failure result.
     */
    public static ImportCoordinatorResult failure(String reason) {
        return ImportCoordinatorResult.builder()
                .canProceed(false)
                .reason(reason)
                .acquiredLock(false)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a result indicating the job is already being processed.
     */
    public static ImportCoordinatorResult alreadyProcessing(String nodeId) {
        return ImportCoordinatorResult.builder()
                .canProceed(false)
                .reason("Job is already being processed by node: " + nodeId)
                .processingNode(nodeId)
                .acquiredLock(false)
                .timestamp(Instant.now())
                .build();
    }
}