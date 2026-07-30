package com.avionics_systems.workflow.engine;

import com.avionics_systems.workflow.dto.TransitionExecutionResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TransitionIdempotencyService {

    private static final long TTL_MS = 300_000;

    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

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

    public void store(String idempotencyKey, TransitionExecutionResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || response == null) {
            return;
        }
        cache.put(idempotencyKey, new CachedResult(response, Instant.now().toEpochMilli()));
    }

    private record CachedResult(TransitionExecutionResponse response, long createdAt) {}
}
