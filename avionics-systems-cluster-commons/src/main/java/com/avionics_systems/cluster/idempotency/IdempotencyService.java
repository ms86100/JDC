package com.avionics_systems.cluster.idempotency;

import com.avionics_systems.cluster.config.ClusterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Slf4j
public class IdempotencyService {

    private final String keyPrefix;
    private final Duration defaultTtl;
    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate, ClusterProperties properties) {
        this.redisTemplate = redisTemplate;
        ClusterProperties.IdempotencyConfig config = properties.getIdempotency();
        this.keyPrefix = config.getKeyPrefix();
        this.defaultTtl = Duration.ofMinutes(config.getTtlMinutes());
    }

    public boolean isProcessed(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(keyPrefix + idempotencyKey));
    }

    public boolean tryProcess(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }
        Boolean set = redisTemplate.opsForValue().setIfAbsent(keyPrefix + idempotencyKey, "1", defaultTtl);
        return Boolean.TRUE.equals(set);
    }

    public String getResult(String idempotencyKey) {
        return redisTemplate.opsForValue().get(keyPrefix + idempotencyKey);
    }

    public void storeResult(String idempotencyKey, String result) {
        redisTemplate.opsForValue().set(keyPrefix + idempotencyKey, result, defaultTtl);
    }
}
