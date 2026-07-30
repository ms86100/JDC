package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.JobClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for job claim operations.
 */
@Repository
public interface JobClaimRepository extends JpaRepository<JobClaim, String> {

    /**
     * Find claim by job ID.
     */
    Optional<JobClaim> findByJobId(String jobId);

    /**
     * Find all claims for a node.
     */
    List<JobClaim> findByNodeId(String nodeId);

    /**
     * Find all active (non-expired) claims for a node.
     */
    @Query("SELECT j FROM JobClaim j WHERE j.nodeId = :nodeId AND j.expiresAt > :now")
    List<JobClaim> findActiveClaimsByNode(@Param("nodeId") String nodeId, @Param("now") Instant now);

    /**
     * Check if a job is currently claimed.
     */
    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM JobClaim j " +
           "WHERE j.jobId = :jobId AND j.expiresAt > :now")
    boolean isJobClaimed(@Param("jobId") String jobId, @Param("now") Instant now);

    /**
     * Find claim for a specific node (if valid).
     */
    @Query("SELECT j FROM JobClaim j WHERE j.jobId = :jobId AND j.nodeId = :nodeId AND j.expiresAt > :now")
    Optional<JobClaim> findValidClaim(@Param("jobId") String jobId, @Param("nodeId") String nodeId, @Param("now") Instant now);

    /**
     * Release a job claim.
     */
    @Modifying
    @Query("DELETE FROM JobClaim j WHERE j.jobId = :jobId AND j.lockId = :lockId")
    int releaseClaim(@Param("jobId") String jobId, @Param("lockId") String lockId);

    /**
     * Release all claims for a node.
     */
    @Modifying
    @Query("DELETE FROM JobClaim j WHERE j.nodeId = :nodeId")
    int releaseAllClaimsByNode(@Param("nodeId") String nodeId);

    /**
     * Clean up expired claims.
     */
    @Modifying
    @Query("DELETE FROM JobClaim j WHERE j.expiresAt < :now")
    int cleanupExpiredClaims(@Param("now") Instant now);

    /**
     * Extend claim expiration (only if owned by the caller).
     */
    @Modifying
    @Query("UPDATE JobClaim j SET j.expiresAt = :newExpiry, j.lastUpdate = :now " +
           "WHERE j.jobId = :jobId AND j.lockId = :lockId AND j.ownerId = :ownerId")
    int extendClaim(@Param("jobId") String jobId,
                     @Param("lockId") String lockId,
                     @Param("ownerId") String ownerId,
                     @Param("newExpiry") Instant newExpiry,
                     @Param("now") Instant now);

    /**
     * Update claim progress.
     */
    @Modifying
    @Query("UPDATE JobClaim j SET j.progressPercentage = :progress, j.lastUpdate = :now " +
           "WHERE j.jobId = :jobId AND j.nodeId = :nodeId")
    int updateProgress(@Param("jobId") String jobId,
                       @Param("nodeId") String nodeId,
                       @Param("progress") Double progress,
                       @Param("now") Instant now);

    /**
     * Find all expired claims.
     */
    @Query("SELECT j FROM JobClaim j WHERE j.expiresAt < :now")
    List<JobClaim> findAllExpired(@Param("now") Instant now);

    /**
     * Count active claims for monitoring.
     */
    @Query("SELECT COUNT(j) FROM JobClaim j WHERE j.expiresAt > :now")
    long countActiveClaims(@Param("now") Instant now);

    /**
     * Count claims by node.
     */
    long countByNodeId(String nodeId);

    /**
     * Find all active claims.
     */
    @Query("SELECT j FROM JobClaim j WHERE j.expiresAt > :now")
    List<JobClaim> findAllActiveClaims(@Param("now") Instant now);
}