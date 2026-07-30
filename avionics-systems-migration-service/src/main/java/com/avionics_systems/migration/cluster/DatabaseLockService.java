package com.avionics_systems.migration.cluster;

import com.avionics_systems.migration.config.ClusterConfig;
import com.avionics_systems.migration.entity.DistributedLock;
import com.avionics_systems.migration.repository.DistributedLockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Database-based implementation of distributed locking.
 * Uses SELECT FOR UPDATE with row locking for atomic lock acquisition.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cluster.lock.type", havingValue = "DATABASE", matchIfMissing = true)
public class DatabaseLockService implements DistributedLockService {

    private static final String LOCK_PREFIX = "lock:";
    private static final String LOCK_TYPE = "EXCLUSIVE";

    @Autowired
    private DistributedLockRepository lockRepository;

    @Autowired
    private ClusterConfig clusterConfig;

    @Autowired
    private ClusterNodeRegistry nodeRegistry;

    /**
     * Generate a unique lock ID.
     */
    private String generateLockId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get the node ID for the current node.
     */
    private String getNodeId() {
        return clusterConfig.getNodeId();
    }

    /**
     * Get or create an owner ID for this node.
     */
    private String getOwnerId() {
        ClusterNode thisNode = nodeRegistry.getThisNode();
        return thisNode != null ? thisNode.getNodeId() : getNodeId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<LockHandle> tryAcquireLock(String resource, Duration ttl) {
        String lockId = generateLockId();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        try {
            // First, check if there's an existing non-expired lock
            Optional<DistributedLock> existingLock = lockRepository.findByResourceName(resource);
            if (existingLock.isPresent()) {
                DistributedLock lock = existingLock.get();
                if (!lock.isExpired()) {
                    log.debug("Lock for resource {} is already held by {}", resource, lock.getOwnerId());
                    return Optional.empty();
                }
                // Clean up expired lock
                lockRepository.delete(lock);
            }

            // Create new lock
            DistributedLock newLock = DistributedLock.builder()
                    .resourceName(resource)
                    .lockId(lockId)
                    .ownerId(getOwnerId())
                    .nodeId(getNodeId())
                    .acquiredAt(now)
                    .expiresAt(expiresAt)
                    .lockType(LOCK_TYPE)
                    .retryCount(0)
                    .build();

            lockRepository.save(newLock);
            log.info("Acquired lock for resource {} with ID {} (TTL: {})", resource, lockId, ttl);

            LockHandle handle = LockHandle.builder()
                    .resource(resource)
                    .lockId(lockId)
                    .ownerId(getOwnerId())
                    .nodeId(getNodeId())
                    .acquiredAt(now)
                    .expiresAt(expiresAt)
                    .lockType(LOCK_TYPE)
                    .build();

            return Optional.of(handle);

        } catch (DataIntegrityViolationException e) {
            // Another node acquired the lock concurrently
            log.debug("Concurrent lock acquisition for resource {}: {}", resource, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error acquiring lock for resource {}: {}", resource, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public LockHandle acquireLock(String resource, Duration ttl, Duration waitTimeout) {
        Instant startTime = Instant.now();
        int attempts = 0;

        while (Duration.between(startTime, Instant.now()).compareTo(waitTimeout) < 0) {
            Optional<LockHandle> handleOpt = tryAcquireLock(resource, ttl);
            if (handleOpt.isPresent()) {
                return handleOpt.get();
            }

            attempts++;
            if (attempts % 10 == 0) {
                log.debug("Waiting for lock on resource {} (attempts: {})", resource, attempts);
            }

            // Wait before retrying
            try {
                Thread.sleep(clusterConfig.getLock().getRetryDelayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("Interrupted while waiting for lock: " + resource, e);
            }
        }

        throw new LockAcquisitionException(
                "Timeout waiting for lock on resource: " + resource + " after " + waitTimeout);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLock(LockHandle handle) {
        if (handle == null) {
            return;
        }
        releaseLock(handle.getResource(), handle.getLockId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLock(String resource, String lockId) {
        try {
            // Use DELETE with conditions to ensure we only delete our own lock
            int deleted = lockRepository.releaseLock(resource, lockId);
            if (deleted > 0) {
                log.info("Released lock for resource {} with ID {}", resource, lockId);
            } else {
                log.warn("Lock not found or not owned for resource {} with ID {}", resource, lockId);
            }
        } catch (Exception e) {
            log.error("Error releasing lock for resource {}: {}", resource, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean isLocked(String resource) {
        Optional<DistributedLock> lock = lockRepository.findByResourceName(resource);
        return lock.isPresent() && !lock.get().isExpired();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean extendLock(LockHandle handle, Duration additionalTtl) {
        if (handle == null) {
            return false;
        }

        Instant newExpiry = Instant.now().plus(additionalTtl);
        int updated = lockRepository.extendLock(
                handle.getResource(),
                handle.getLockId(),
                handle.getOwnerId(),
                newExpiry
        );

        if (updated > 0) {
            log.info("Extended lock for resource {} by {}", handle.getResource(), additionalTtl);
            return true;
        }

        log.warn("Failed to extend lock for resource {} - lock not found or not owned", handle.getResource());
        return false;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<LockInfo> getLockInfo(String resource) {
        return lockRepository.findByResourceName(resource)
                .filter(lock -> !lock.isExpired())
                .map(lock -> LockInfo.builder()
                        .resource(lock.getResourceName())
                        .lockId(lock.getLockId())
                        .ownerId(lock.getOwnerId())
                        .nodeId(lock.getNodeId())
                        .acquiredAt(lock.getAcquiredAt())
                        .expiresAt(lock.getExpiresAt())
                        .isHeld(true)
                        .lockType(lock.getLockType())
                        .remainingTtlSeconds(lock.getRemainingTtlSeconds())
                        .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseAllLocks() {
        String nodeId = getNodeId();
        try {
            int released = lockRepository.releaseAllLocksByNode(nodeId);
            log.info("Released {} locks for node {}", released, nodeId);
        } catch (Exception e) {
            log.error("Error releasing all locks for node {}: {}", nodeId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Iterable<LockInfo> getLocksHeldByThisNode() {
        String nodeId = getNodeId();
        List<DistributedLock> locks = lockRepository.findByNodeId(nodeId);
        List<LockInfo> lockInfos = new ArrayList<>();

        for (DistributedLock lock : locks) {
            if (!lock.isExpired()) {
                lockInfos.add(LockInfo.builder()
                        .resource(lock.getResourceName())
                        .lockId(lock.getLockId())
                        .ownerId(lock.getOwnerId())
                        .nodeId(lock.getNodeId())
                        .acquiredAt(lock.getAcquiredAt())
                        .expiresAt(lock.getExpiresAt())
                        .isHeld(true)
                        .lockType(lock.getLockType())
                        .remainingTtlSeconds(lock.getRemainingTtlSeconds())
                        .build());
            }
        }

        return lockInfos;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int cleanupExpiredLocks() {
        try {
            int cleaned = lockRepository.cleanupExpiredLocks(Instant.now());
            if (cleaned > 0) {
                log.info("Cleaned up {} expired locks", cleaned);
            }
            return cleaned;
        } catch (Exception e) {
            log.error("Error cleaning up expired locks: {}", e.getMessage(), e);
            return 0;
        }
    }
}