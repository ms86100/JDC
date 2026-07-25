package com.jira.cluster.health;

import com.jira.cluster.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@RequiredArgsConstructor
public class StorageHealthIndicator implements HealthIndicator {

    private final StorageProvider storageProvider;

    @Override
    public Health health() {
        try {
            boolean exists = storageProvider.exists(".health-check");
            return Health.up().withDetail("storage", "accessible").build();
        } catch (Exception e) {
            return Health.down().withDetail("storage", e.getMessage()).build();
        }
    }
}
