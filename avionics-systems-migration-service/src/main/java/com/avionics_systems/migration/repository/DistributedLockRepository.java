package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.DistributedLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for distributed lock operations.
 */
@Repository
public interface DistributedLockRepository extends JpaRepository<DistributedLock, String> {

    /**
     * Find a lock by resource name.
     */
    Optional<DistributedLock> findByResourceName(String resourceName);

    /**
     * Find all locks owned by a specific node.
     */
    List<DistributedLock> findByNodeId(String nodeId);

    /**
     * Find all locks owned by a specific owner.
     */
    List<DistributedLock> findByOwnerId(String ownerId);

    /**
     * Release a lock by resource and lock ID (only if owned by the caller).
     */
    @Modifying
    @Query("DELETE FROM DistributedLock l WHERE l.resourceName = :resource AND l.lockId = :lockId")
    int releaseLock(@Param("resource") String resource, @Param("lockId") String lockId);

    /**
     * Release all locks owned by a specific node.
     */
    @Modifying
    @Query("DELETE FROM DistributedLock l WHERE l.nodeId = :nodeId")
    int releaseAllLocksByNode(@Param("nodeId") String nodeId);

    /**
     * Clean up expired locks.
     */
    @Modifying
    @Query("DELETE FROM DistributedLock l WHERE l.expiresAt < :now")
    int cleanupExpiredLocks(@Param("now") Instant now);

    /**
     * Count expired locks for monitoring.
     */
    @Query("SELECT COUNT(l) FROM DistributedLock l WHERE l.expiresAt < :now")
    long countExpiredLocks(@Param("now") Instant now);

    /**
     * Check if a resource is locked.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM DistributedLock l " +
           "WHERE l.resourceName = :resource AND l.expiresAt > :now")
    boolean isLocked(@Param("resource") String resource, @Param("now") Instant now);

    /**
     * Extend lock expiration (only if owned by the caller).
     */
    @Modifying
    @Query("UPDATE DistributedLock l SET l.expiresAt = :newExpiry WHERE l.resourceName = :resource " +
           "AND l.lockId = :lockId AND l.ownerId = :ownerId")
    int extendLock(@Param("resource") String resource,
                    @Param("lockId") String lockId,
                    @Param("ownerId") String ownerId,
                    @Param("newExpiry") Instant newExpiry);

    /**
     * Find all active (non-expired) locks.
     */
    @Query("SELECT l FROM DistributedLock l WHERE l.expiresAt > :now")
    List<DistributedLock> findAllActiveLocks(@Param("now") Instant now);

    /**
     * Delete locks by owner ID (for cleanup).
     */
    @Modifying
    @Query("DELETE FROM DistributedLock l WHERE l.ownerId = :ownerId")
    int deleteByOwnerId(@Param("ownerId") String ownerId);
}