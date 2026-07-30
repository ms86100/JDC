package com.avionics_systems.migration.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.config.ClusterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-based implementation of distributed locking.
 * Uses SET NX with TTL for atomic lock acquisition and Lua scripts for safe unlock.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cluster.lock.type", havingValue = "REDIS")
public class RedisLockService implements DistributedLockService {

    private static final String LOCK_PREFIX = "lock:";
    private static final String LOCK_TYPE = "EXCLUSIVE";

    // Lua script for safe unlock - only delete if we own the lock
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    // Lua script for lock extension - only extend if we own the lock
    private static final String EXTEND_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('pexpire', KEYS[1], ARGV[2]) " +
            "else " +
            "    return 0 " +
            "end";

    private final StringRedisTemplate redisTemplate;
    private final ClusterConfig clusterConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    public RedisLockService(StringRedisTemplate redisTemplate, ClusterConfig clusterConfig) {
        this.redisTemplate = redisTemplate;
        this.clusterConfig = clusterConfig;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate a unique lock ID.
     */
    private String generateLockId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get Redis key for a resource.
     */
    private String getLockKey(String resource) {
        return LOCK_PREFIX + resource;
    }

    /**
     * Create lock metadata JSON.
     */
    private String createLockValue(LockHandle handle) {
        try {
            return objectMapper.writeValueAsString(handle);
        } catch (JsonProcessingException e) {
            log.error("Error serializing lock handle", e);
            return handle.getLockId();
        }
    }

    /**
     * Parse lock metadata from JSON.
     */
    private LockHandle parseLockValue(String json, String resource) {
        try {
            return objectMapper.readValue(json, LockHandle.class);
        } catch (JsonProcessingException e) {
            // Fallback: create handle from lock ID
            log.warn("Could not parse lock value, creating minimal handle");
            return LockHandle.builder()
                    .resource(resource)
                    .lockId(json)
                    .build();
        }
    }

    @Override
    public Optional<LockHandle> tryAcquireLock(String resource, Duration ttl) {
        String lockKey = getLockKey(resource);
        String lockId = generateLockId();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        // Create lock handle
        LockHandle handle = LockHandle.builder()
                .resource(resource)
                .lockId(lockId)
                .ownerId(getOwnerId())
                .nodeId(getNodeId())
                .acquiredAt(now)
                .expiresAt(expiresAt)
                .lockType(LOCK_TYPE)
                .build();

        String lockValue = createLockValue(handle);

        try {
            // Try to set the lock with NX (only if not exists) and TTL
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, ttl);

            if (Boolean.TRUE.equals(acquired)) {
                log.info("Acquired Redis lock for resource {} with ID {} (TTL: {})", resource, lockId, ttl);
                return Optional.of(handle);
            } else {
                log.debug("Lock for resource {} is already held", resource);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error acquiring Redis lock for resource {}: {}", resource, e.getMessage(), e);
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
                log.debug("Waiting for Redis lock on resource {} (attempts: {})", resource, attempts);
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
    public void releaseLock(LockHandle handle) {
        if (handle == null) {
            return;
        }

        String lockKey = getLockKey(handle.getResource());
        String lockValue = createLockValue(handle);

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);

            if (result != null && result > 0) {
                log.info("Released Redis lock for resource {} with ID {}", handle.getResource(), handle.getLockId());
            } else {
                log.warn("Redis lock not found or not owned for resource {} with ID {}",
                        handle.getResource(), handle.getLockId());
            }
        } catch (Exception e) {
            log.error("Error releasing Redis lock for resource {}: {}", handle.getResource(), e.getMessage(), e);
        }
    }

    @Override
    public void releaseLock(String resource, String lockId) {
        String lockKey = getLockKey(resource);

        try {
            // First check if we own the lock
            String currentValue = redisTemplate.opsForValue().get(lockKey);
            if (currentValue != null) {
                LockHandle handle = parseLockValue(currentValue, resource);
                if (handle.getLockId().equals(lockId)) {
                    redisTemplate.delete(lockKey);
                    log.info("Released Redis lock for resource {} with ID {}", resource, lockId);
                } else {
                    log.warn("Lock ID mismatch for resource {}: expected {}, found {}",
                            resource, lockId, handle.getLockId());
                }
            }
        } catch (Exception e) {
            log.error("Error releasing Redis lock for resource {}: {}", resource, e.getMessage(), e);
        }
    }

    @Override
    public boolean isLocked(String resource) {
        String lockKey = getLockKey(resource);
        try {
            String value = redisTemplate.opsForValue().get(lockKey);
            if (value == null) {
                return false;
            }

            // Check if the lock has expired based on TTL
            Long ttl = redisTemplate.getExpire(lockKey);
            return ttl != null && ttl > 0;
        } catch (Exception e) {
            log.error("Error checking Redis lock for resource {}: {}", resource, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean extendLock(LockHandle handle, Duration additionalTtl) {
        if (handle == null) {
            return false;
        }

        String lockKey = getLockKey(handle.getResource());
        String lockValue = createLockValue(handle);
        long ttlMillis = additionalTtl.toMillis();

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(EXTEND_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue, String.valueOf(ttlMillis));

            if (result != null && result > 0) {
                log.info("Extended Redis lock for resource {} by {}", handle.getResource(), additionalTtl);
                return true;
            }

            log.warn("Failed to extend Redis lock for resource {} - lock not found or not owned",
                    handle.getResource());
            return false;
        } catch (Exception e) {
            log.error("Error extending Redis lock for resource {}: {}", handle.getResource(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<LockInfo> getLockInfo(String resource) {
        String lockKey = getLockKey(resource);

        try {
            String value = redisTemplate.opsForValue().get(lockKey);
            if (value == null) {
                return Optional.empty();
            }

            LockHandle handle = parseLockValue(value, resource);
            Long ttl = redisTemplate.getExpire(lockKey);

            Instant expiresAt = ttl != null && ttl > 0
                    ? Instant.now().plusMillis(ttl)
                    : Instant.now();

            return Optional.of(LockInfo.builder()
                    .resource(handle.getResource())
                    .lockId(handle.getLockId())
                    .ownerId(handle.getOwnerId())
                    .nodeId(handle.getNodeId())
                    .acquiredAt(handle.getAcquiredAt())
                    .expiresAt(expiresAt)
                    .isHeld(true)
                    .lockType(handle.getLockType())
                    .remainingTtlSeconds(ttl != null ? ttl / 1000 : 0)
                    .build());
        } catch (Exception e) {
            log.error("Error getting Redis lock info for resource {}: {}", resource, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void releaseAllLocks() {
        String pattern = LOCK_PREFIX + "*";
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                String nodeId = getNodeId();
                int released = 0;

                for (String key : keys) {
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        LockHandle handle = parseLockValue(value, key.replace(LOCK_PREFIX, ""));
                        if (nodeId.equals(handle.getNodeId())) {
                            redisTemplate.delete(key);
                            released++;
                        }
                    }
                }

                log.info("Released {} Redis locks for node {}", released, nodeId);
            }
        } catch (Exception e) {
            log.error("Error releasing all Redis locks: {}", e.getMessage(), e);
        }
    }

    @Override
    public Iterable<LockInfo> getLocksHeldByThisNode() {
        // Not efficiently implementable in Redis without scanning
        // For production, consider using Redis SET for tracking locks per node
        return Collections.emptyList();
    }

    @Override
    public int cleanupExpiredLocks() {
        // Redis handles TTL automatically, so no cleanup needed
        return 0;
    }

    private String getNodeId() {
        return clusterConfig.getNodeId();
    }

    private String getOwnerId() {
        return clusterConfig.getNodeId();
    }
}