package com.avionics_systems.cluster.health;

import com.avionics_systems.cluster.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@RequiredArgsConstructor
public class StorageHealthIndicator implements HealthIndicator {

    private final StorageProvider storageProvider;
    private final String healthCheckPath;

    @Override
    public Health health() {
        try {
            boolean exists = storageProvider.exists(healthCheckPath);
            return Health.up().withDetail("storage", "accessible").build();
        } catch (Exception e) {
            return Health.down().withDetail("storage", e.getMessage()).build();
        }
    }
}
