package com.jira.migration.service.clients;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for all service client properties and common beans.
 */
@Configuration
@ConfigurationProperties(prefix = "services")
@Data
@Slf4j
public class ServiceClientsConfig {

    private String issueServiceUrl = "http://localhost:8081";
    private String projectServiceUrl = "http://localhost:8082";
    private String userServiceUrl = "http://localhost:8083";
    private String workflowServiceUrl = "http://localhost:8084";
    private String searchServiceUrl = "http://localhost:8085";
    private String notificationServiceUrl = "http://localhost:8086";
    private String sprintServiceUrl = "http://localhost:8087";
    private String commentServiceUrl = "http://localhost:8088";
    private String attachmentServiceUrl = "http://localhost:8089";
    private String auditServiceUrl = "http://localhost:8090";

    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
    private int maxRetries = 3;

    @Bean
    @Primary
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestTemplate restTemplate = builder
                .requestFactory(() -> new BufferingClientHttpRequestFactory(factory))
                .build();

        log.info("RestTemplate configured with connectTimeout={}ms, readTimeout={}ms",
                connectTimeoutMs, readTimeoutMs);
        return restTemplate;
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Register circuit breakers for each service
        registerCircuitBreaker(registry, "issueService", issueServiceUrl);
        registerCircuitBreaker(registry, "projectService", projectServiceUrl);
        registerCircuitBreaker(registry, "userService", userServiceUrl);
        registerCircuitBreaker(registry, "workflowService", workflowServiceUrl);
        registerCircuitBreaker(registry, "searchService", searchServiceUrl);
        registerCircuitBreaker(registry, "notificationService", notificationServiceUrl);
        registerCircuitBreaker(registry, "sprintService", sprintServiceUrl);
        registerCircuitBreaker(registry, "commentService", commentServiceUrl);
        registerCircuitBreaker(registry, "attachmentService", attachmentServiceUrl);
        registerCircuitBreaker(registry, "auditService", auditServiceUrl);

        log.info("CircuitBreakerRegistry configured with {} circuit breakers",
                registry.getAllCircuitBreakers().size());
        return registry;
    }

    private void registerCircuitBreaker(CircuitBreakerRegistry registry, String name, String baseUrl) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker(name);
        log.debug("Registered circuit breaker '{}' for service at '{}'", name, baseUrl);
    }

    /**
     * Map of service names to their base URLs for dynamic lookups.
     */
    @Bean
    public Map<String, String> serviceUrlMap() {
        Map<String, String> map = new HashMap<>();
        map.put("issueService", issueServiceUrl);
        map.put("projectService", projectServiceUrl);
        map.put("userService", userServiceUrl);
        map.put("workflowService", workflowServiceUrl);
        map.put("searchService", searchServiceUrl);
        map.put("notificationService", notificationServiceUrl);
        map.put("sprintService", sprintServiceUrl);
        map.put("commentService", commentServiceUrl);
        map.put("attachmentService", attachmentServiceUrl);
        map.put("auditService", auditServiceUrl);
        return map;
    }
}
