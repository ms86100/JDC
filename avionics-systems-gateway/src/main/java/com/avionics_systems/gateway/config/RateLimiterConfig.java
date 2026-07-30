package com.avionics_systems.gateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimiterConfig {

    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${ratelimit.requests-per-minute:100}")
    private int defaultRequestsPerMinute;

    @Value("${ratelimit.requests-per-hour:1000}")
    private int defaultRequestsPerHour;

    @Value("${ratelimit.requests-per-day:10000}")
    private int defaultRequestsPerDay;

    @Value("${app.gateway.redis-key-prefix:ratelimit:}")
    private String redisKeyPrefix;

    public Bucket resolveBucket(String key) {
        return localBuckets.computeIfAbsent(key, this::createNewBucket);
    }

    public Bucket resolveBucket(String key, int requestsPerMinute) {
        return localBuckets.computeIfAbsent(key, k -> createNewBucket(requestsPerMinute));
    }

    private Bucket createNewBucket(String key) {
        return createNewBucket(defaultRequestsPerMinute);
    }

    private Bucket createNewBucket(int requestsPerMinute) {
        Bandwidth minuteLimit = Bandwidth.classic(
                requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );

        Bandwidth hourlyLimit = Bandwidth.classic(
                defaultRequestsPerHour,
                Refill.greedy(defaultRequestsPerHour, Duration.ofHours(1))
        );

        Bandwidth dailyLimit = Bandwidth.classic(
                defaultRequestsPerDay,
                Refill.greedy(defaultRequestsPerDay, Duration.ofDays(1))
        );

        return Bucket.builder()
                .addLimit(minuteLimit)
                .addLimit(hourlyLimit)
                .addLimit(dailyLimit)
                .build();
    }

    public boolean isAllowed(String key) {
        if (redisTemplate != null) {
            return isAllowedViaRedis(key);
        }
        return resolveBucket(key).tryConsume(1);
    }

    private boolean isAllowedViaRedis(String key) {
        try {
            String redisKey = redisKeyPrefix + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofMinutes(1));
            }
            return count != null && count <= defaultRequestsPerMinute;
        } catch (Exception e) {
            log.debug("Redis rate limit check failed, falling back to local: {}", e.getMessage());
            return resolveBucket(key).tryConsume(1);
        }
    }

    public void clearAllBuckets() {
        localBuckets.clear();
    }

    public void removeBucket(String key) {
        localBuckets.remove(key);
    }

    public int getBucketCount() {
        return localBuckets.size();
    }
}
