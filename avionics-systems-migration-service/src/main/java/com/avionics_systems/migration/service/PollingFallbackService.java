package com.avionics_systems.migration.service;

import com.avionics_systems.migration.dto.JobProgressResponse;
import com.avionics_systems.migration.websocket.dto.JobProgressUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides polling fallback for clients that don't support WebSocket.
 * Caches job progress for efficient polling.
 */
@Service
@Slf4j
public class PollingFallbackService {

    @Value("${polling.cache-ttl-minutes:5}")
    private int cacheTtlMinutes;

    @Value("${polling.max-poll-interval-ms:2000}")
    private int maxPollIntervalMs;

    // Cache: jobId -> CachedProgress
    private final Map<String, CachedProgress> progressCache = new ConcurrentHashMap<>();

    // Track last poll time per client for rate limiting
    private final Map<String, Instant> lastPollTimes = new ConcurrentHashMap<>();

    // Default TTL for cached items
    private Duration getCacheTtl() {
        return Duration.ofMinutes(cacheTtlMinutes);
    }

    /**
     * Cache a progress update for polling
     */
    public void cacheProgress(String jobId, JobProgressUpdate update) {
        progressCache.put(jobId, new CachedProgress(update, Instant.now()));
        log.debug("Cached progress for job {}: {}%", jobId, update.getProgressPercentage());
    }

    /**
     * Cache progress from JobProgressResponse
     */
    public void cacheProgress(JobProgressResponse response) {
        JobProgressUpdate update = JobProgressUpdate.fromProgressResponse(response);
        cacheProgress(response.getJobId().toString(), update);
    }

    /**
     * Get cached progress for a job
     */
    public Optional<JobProgressUpdate> getCachedProgress(String jobId) {
        CachedProgress cached = progressCache.get(jobId);

        if (cached == null) {
            return Optional.empty();
        }

        // Check if cache is still valid
        if (cached.cachedAt().plus(getCacheTtl()).isBefore(Instant.now())) {
            // Cache expired, remove it
            progressCache.remove(jobId);
            return Optional.empty();
        }

        return Optional.of(cached.progress());
    }

    /**
     * Get cached progress with rate limiting check
     * Returns empty if client is polling too frequently
     */
    public Optional<JobProgressUpdate> getCachedProgress(String jobId, String clientId) {
        if (isRateLimited(clientId)) {
            log.debug("Client {} rate limited for job {}", clientId, jobId);
            return Optional.empty();
        }

        lastPollTimes.put(clientId, Instant.now());
        return getCachedProgress(jobId);
    }

    /**
     * Invalidate cache for a job
     */
    public void invalidateCache(String jobId) {
        progressCache.remove(jobId);
        log.debug("Invalidated cache for job {}", jobId);
    }

    /**
     * Invalidate all caches (e.g., on service restart)
     */
    public void invalidateAllCaches() {
        progressCache.clear();
        log.info("All progress caches invalidated");
    }

    /**
     * Check if a client is being rate limited
     */
    private boolean isRateLimited(String clientId) {
        Instant lastPoll = lastPollTimes.get(clientId);
        if (lastPoll == null) {
            return false;
        }

        long elapsedMs = Duration.between(lastPoll, Instant.now()).toMillis();
        return elapsedMs < maxPollIntervalMs;
    }

    /**
     * Get time remaining until next poll is allowed
     */
    public long getTimeUntilNextPollAllowed(String clientId) {
        Instant lastPoll = lastPollTimes.get(clientId);
        if (lastPoll == null) {
            return 0;
        }

        long elapsedMs = Duration.between(lastPoll, Instant.now()).toMillis();
        return Math.max(0, maxPollIntervalMs - elapsedMs);
    }

    /**
     * Refresh cache if stale (older than half TTL)
     */
    public void refreshIfStale(String jobId, java.util.function.Supplier<JobProgressUpdate> refreshFn) {
        CachedProgress cached = progressCache.get(jobId);
        Duration halfTtl = getCacheTtl().dividedBy(2);

        if (cached == null || cached.cachedAt().plus(halfTtl).isBefore(Instant.now())) {
            try {
                JobProgressUpdate updated = refreshFn.get();
                cacheProgress(jobId, updated);
            } catch (Exception e) {
                log.error("Failed to refresh stale cache for job {}: {}", jobId, e.getMessage());
            }
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
                "cachedJobs", progressCache.size(),
                "oldestCacheAge", getOldestCacheAgeSeconds(),
                "rateLimitMs", maxPollIntervalMs
        );
    }

    private long getOldestCacheAgeSeconds() {
        return progressCache.values().stream()
                .mapToLong(c -> Duration.between(c.cachedAt(), Instant.now()).toSeconds())
                .max()
                .orElse(0);
    }

    // Helper record for cached progress
    private record CachedProgress(JobProgressUpdate progress, Instant cachedAt) {}
}