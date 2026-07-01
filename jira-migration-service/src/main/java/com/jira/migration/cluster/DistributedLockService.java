package com.jira.migration.cluster;

import java.time.Duration;
import java.util.Optional;

/**
 * Service for managing distributed locks across the cluster.
 * Provides atomic lock acquisition, release, and extension capabilities.
 */
public interface DistributedLockService {

    /**
     * Try to acquire a lock for the given resource without waiting.
     *
     * @param resource The resource to lock
     * @param ttl      Time-to-live for the lock
     * @return Optional containing the lock handle if acquired, empty otherwise
     */
    Optional<LockHandle> tryAcquireLock(String resource, Duration ttl);

    /**
     * Acquire a lock for the given resource, waiting if necessary.
     *
     * @param resource     The resource to lock
     * @param ttl          Time-to-live for the lock
     * @param waitTimeout  Maximum time to wait for lock acquisition
     * @return LockHandle if acquired, throws exception if timeout
     */
    LockHandle acquireLock(String resource, Duration ttl, Duration waitTimeout);

    /**
     * Release a previously acquired lock.
     *
     * @param handle The lock handle to release
     */
    void releaseLock(LockHandle handle);

    /**
     * Release a lock by resource and lock ID.
     *
     * @param resource The resource name
     * @param lockId   The lock ID
     */
    void releaseLock(String resource, String lockId);

    /**
     * Check if a resource is currently locked.
     *
     * @param resource The resource to check
     * @return true if the resource is locked, false otherwise
     */
    boolean isLocked(String resource);

    /**
     * Extend the TTL of a previously acquired lock.
     *
     * @param handle        The lock handle
     * @param additionalTtl Additional time to add to the lock
     * @return true if extended successfully, false otherwise
     */
    boolean extendLock(LockHandle handle, Duration additionalTtl);

    /**
     * Get information about a lock.
     *
     * @param resource The resource name
     * @return LockInfo if the lock exists, empty otherwise
     */
    Optional<LockInfo> getLockInfo(String resource);

    /**
     * Release all locks held by this node.
     */
    void releaseAllLocks();

    /**
     * Get all locks held by this node.
     *
     * @return Iterable of LockInfo for locks held by this node
     */
    Iterable<LockInfo> getLocksHeldByThisNode();

    /**
     * Clean up expired locks.
     *
     * @return Number of locks cleaned up
     */
    int cleanupExpiredLocks();

    /**
     * Execute an operation with a distributed lock.
     *
     * @param resource   The resource to lock
     * @param ttl        Lock TTL
     * @param operation  The operation to execute
     * @param <T>        Return type of the operation
     * @return Result of the operation
     */
    default <T> T executeWithLock(String resource, Duration ttl, java.util.function.Supplier<T> operation) {
        Optional<LockHandle> handleOpt = tryAcquireLock(resource, ttl);
        if (handleOpt.isEmpty()) {
            throw new LockAcquisitionException("Failed to acquire lock for resource: " + resource);
        }
        try {
            return operation.get();
        } finally {
            releaseLock(handleOpt.get());
        }
    }

    /**
     * Execute an operation with a distributed lock (void return).
     */
    default void executeWithLock(String resource, Duration ttl, Runnable operation) {
        executeWithLock(resource, ttl, () -> {
            operation.run();
            return null;
        });
    }
}