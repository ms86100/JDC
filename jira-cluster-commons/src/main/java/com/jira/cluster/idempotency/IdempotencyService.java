package com.jira.cluster.idempotency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Slf4j
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isProcessed(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + idempotencyKey));
    }

    public boolean tryProcess(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }
        Boolean set = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + idempotencyKey, "1", DEFAULT_TTL);
        return Boolean.TRUE.equals(set);
    }

    public String getResult(String idempotencyKey) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
    }

    public void storeResult(String idempotencyKey, String result) {
        redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, result, DEFAULT_TTL);
    }
}
