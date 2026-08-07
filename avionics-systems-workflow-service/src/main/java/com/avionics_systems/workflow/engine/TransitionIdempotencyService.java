package com.avionics_systems.workflow.engine;

import com.avionics_systems.workflow.dto.TransitionExecutionResponse;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
public class TransitionIdempotencyService {

    private static final long TTL_MS = 300_000;
    private static final int MAX_CACHE_SIZE = 10_000;

    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    /**
     * Atomically check whether a key is already reserved or completed.
     * Uses {@link ConcurrentHashMap#putIfAbsent} with an in-progress sentinel
     * (null response) so that two concurrent requests cannot both see "not present"
     * and both execute the transition.
     *
     * @return {@code null} if the caller should proceed (key was successfully reserved);
     *         a {@link TransitionExecutionResponse} if a previous result exists or the
     *         request is already in progress.
     */
    public TransitionExecutionResponse checkAndReserve(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null; // no key => no dedup, caller proceeds
        }

        if (cache.size() >= MAX_CACHE_SIZE) {
            return TransitionExecutionResponse.builder()
                    .success(false)
                    .error("Idempotency cache full; please retry later")
                    .build();
        }

        CachedResult sentinel = new CachedResult(null, Instant.now().toEpochMilli());
        CachedResult existing = cache.putIfAbsent(idempotencyKey, sentinel);

        if (existing == null) {
            // We reserved the key; caller should proceed with execution
            return null;
        }

        // Key already present — check if expired
        if (Instant.now().toEpochMilli() - existing.createdAt > TTL_MS) {
            cache.remove(idempotencyKey, existing);
            // Retry once after evicting the expired entry
            existing = cache.putIfAbsent(idempotencyKey, sentinel);
            if (existing == null) {
                return null; // successfully reserved after eviction
            }
        }

        // Key is live — either completed or in-progress
        if (existing.response != null) {
            return existing.response; // completed: return cached result
        }

        // In-progress sentinel (response is null)
        return TransitionExecutionResponse.builder()
                .success(false)
                .error("Duplicate request in progress")
                .build();
    }

    /**
     * Look up a previously stored result. Kept for backward compatibility.
     */
    public TransitionExecutionResponse getIfPresent(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        CachedResult cached = cache.get(idempotencyKey);
        if (cached == null) {
            return null;
        }
        if (Instant.now().toEpochMilli() - cached.createdAt > TTL_MS) {
            cache.remove(idempotencyKey);
            return null;
        }
        return cached.response;
    }

    /**
     * Store the final result, replacing the in-progress sentinel.
     */
    public void store(String idempotencyKey, TransitionExecutionResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || response == null) {
            return;
        }
        cache.put(idempotencyKey, new CachedResult(response, Instant.now().toEpochMilli()));
    }

    /**
     * Periodic cleanup of expired entries so the cache does not grow unbounded.
     */
    @Scheduled(fixedDelay = 60000)
    public void evictExpired() {
        long now = Instant.now().toEpochMilli();
        cache.entrySet().removeIf(e -> now - e.getValue().createdAt > TTL_MS);
    }

    private record CachedResult(TransitionExecutionResponse response, long createdAt) {}
}
