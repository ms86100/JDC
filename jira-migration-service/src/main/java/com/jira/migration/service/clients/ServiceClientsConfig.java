package com.jira.migration.service.clients;

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

    private String issueServiceUrl = "http://localhost:8084";
    private String projectServiceUrl = "http://localhost:8083";
    private String userServiceUrl = "http://localhost:8082";
    private String workflowServiceUrl = "http://localhost:8085";
    private String searchServiceUrl = "http://localhost:8088";
    private String notificationServiceUrl = "http://localhost:8087";
    private String sprintServiceUrl = "http://localhost:8091";
    private String commentServiceUrl = "http://localhost:8086";
    private String attachmentServiceUrl = "http://localhost:8090";
    private String auditServiceUrl = "http://localhost:8089";

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

        // User lookups are optional during CSV import — only trip on real 5xx outages.
        CircuitBreakerConfig userLookupConfig = CircuitBreakerConfig.from(defaultConfig)
                .recordException(e -> {
                    if (e instanceof ServiceClientException sce) {
                        // 404/400 not-found and connection errors must not open the breaker.
                        return sce.getStatusCode() >= 500;
                    }
                    return false;
                })
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        registry.circuitBreaker("userService", userLookupConfig);
        return registry;
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
