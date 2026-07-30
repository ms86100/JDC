package com.avionics_systems.cluster.resilience;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable resilience thresholds for circuit breaker and retry policies.
 *
 * <p>Defaults match the original hardcoded values for zero-regression.</p>
 */
@Data
@ConfigurationProperties(prefix = "cluster.resilience")
public class ResilienceProperties {

    private CircuitBreakerProps circuitBreaker = new CircuitBreakerProps();
    private RetryProps retry = new RetryProps();

    @Data
    public static class CircuitBreakerProps {
        private float failureRateThreshold = 50;
        private float slowCallRateThreshold = 80;
        private int slowCallDurationSeconds = 10;
        private int waitDurationInOpenStateSeconds = 30;
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
        private int permittedNumberOfCallsInHalfOpenState = 3;
    }

    @Data
    public static class RetryProps {
        private int maxAttempts = 3;
        private int waitDurationMillis = 500;
    }
}
