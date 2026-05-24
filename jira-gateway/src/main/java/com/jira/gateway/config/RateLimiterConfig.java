package com.jira.gateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiter using Bucket4j Token Bucket algorithm.
 * Provides configurable rate limiting per client/user.
 */
@Component
public class RateLimiterConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Default limits: 100 requests per minute per user
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final int DEFAULT_REQUESTS_PER_HOUR = 1000;
    private static final int DEFAULT_REQUESTS_PER_DAY = 10000;

    /**
     * Get or create a rate limit bucket for a given key.
     * Uses user ID for authenticated requests, IP address for anonymous.
     */
    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, this::createNewBucket);
    }

    /**
     * Get bucket with custom limits (for premium users, etc.)
     */
    public Bucket resolveBucket(String key, int requestsPerMinute) {
        return buckets.computeIfAbsent(key, k -> createNewBucket(requestsPerMinute));
    }

    private Bucket createNewBucket(String key) {
        return createNewBucket(DEFAULT_REQUESTS_PER_MINUTE);
    }

    private Bucket createNewBucket(int requestsPerMinute) {
        Bandwidth minuteLimit = Bandwidth.classic(
                requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );

        Bandwidth hourlyLimit = Bandwidth.classic(
                DEFAULT_REQUESTS_PER_HOUR,
                Refill.greedy(DEFAULT_REQUESTS_PER_HOUR / 60, Duration.ofHours(1))
        );

        Bandwidth dailyLimit = Bandwidth.classic(
                DEFAULT_REQUESTS_PER_DAY,
                Refill.greedy(DEFAULT_REQUESTS_PER_DAY / 1440, Duration.ofDays(1))
        );

        return Bucket.builder()
                .addLimit(minuteLimit)
                .addLimit(hourlyLimit)
                .addLimit(dailyLimit)
                .build();
    }

    /**
     * Clear all buckets (for testing or reset purposes)
     */
    public void clearAllBuckets() {
        buckets.clear();
    }

    /**
     * Remove a specific bucket
     */
    public void removeBucket(String key) {
        buckets.remove(key);
    }

    /**
     * Get current bucket count
     */
    public int getBucketCount() {
        return buckets.size();
    }
}