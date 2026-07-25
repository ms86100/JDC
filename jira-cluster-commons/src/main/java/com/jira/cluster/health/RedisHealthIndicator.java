package com.jira.cluster.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    @Override
    public Health health() {
        try {
            connectionFactory.getConnection().ping();
            return Health.up().withDetail("redis", "connected").build();
        } catch (Exception e) {
            return Health.down().withDetail("redis", e.getMessage()).build();
        }
    }
}
