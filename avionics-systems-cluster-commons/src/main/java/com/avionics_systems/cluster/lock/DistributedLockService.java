package com.avionics_systems.cluster.lock;

import java.time.Duration;
import java.util.Optional;

public interface DistributedLockService {

    Optional<LockHandle> tryAcquireLock(String resource, Duration ttl);

    LockHandle acquireLock(String resource, Duration ttl, Duration waitTimeout);

    void releaseLock(LockHandle handle);

    void releaseLock(String resource, String lockId);

    boolean isLocked(String resource);

    boolean extendLock(LockHandle handle, Duration additionalTtl);

    Optional<LockInfo> getLockInfo(String resource);

    void releaseAllLocks();

    Iterable<LockInfo> getLocksHeldByThisNode();

    int cleanupExpiredLocks();

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

    default void executeWithLock(String resource, Duration ttl, Runnable operation) {
        executeWithLock(resource, ttl, () -> {
            operation.run();
            return null;
        });
    }
}
