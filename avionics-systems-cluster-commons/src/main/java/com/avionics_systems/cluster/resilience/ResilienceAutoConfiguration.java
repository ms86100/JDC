package com.avionics_systems.cluster.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnClass(CircuitBreakerRegistry.class)
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties properties) {
        ResilienceProperties.CircuitBreakerProps cb = properties.getCircuitBreaker();
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cb.getFailureRateThreshold())
                .slowCallRateThreshold(cb.getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofSeconds(cb.getSlowCallDurationSeconds()))
                .waitDurationInOpenState(Duration.ofSeconds(cb.getWaitDurationInOpenStateSeconds()))
                .slidingWindowSize(cb.getSlidingWindowSize())
                .minimumNumberOfCalls(cb.getMinimumNumberOfCalls())
                .permittedNumberOfCallsInHalfOpenState(cb.getPermittedNumberOfCallsInHalfOpenState())
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryRegistry retryRegistry(ResilienceProperties properties) {
        ResilienceProperties.RetryProps retryProps = properties.getRetry();
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retryProps.getMaxAttempts())
                .waitDuration(Duration.ofMillis(retryProps.getWaitDurationMillis()))
                .retryExceptions(java.io.IOException.class, java.util.concurrent.TimeoutException.class,
                        org.springframework.web.client.ResourceAccessException.class)
                .build();
        return RetryRegistry.of(config);
    }
}
