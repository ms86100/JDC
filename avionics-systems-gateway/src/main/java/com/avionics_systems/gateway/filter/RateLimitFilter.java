package com.avionics_systems.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.avionics_systems.gateway.config.RateLimiterConfig;

import java.util.Arrays;
import java.util.List;

/**
 * Rate Limiting Filter using Bucket4j Token Bucket algorithm.
 * Limits requests per user/IP to prevent abuse.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterConfig rateLimiterConfig;
    private final List<String> rateLimitExemptPaths;
    private final String rateLimitHeaderValue;
    private final String retryAfterSeconds;

    public RateLimitFilter(RateLimiterConfig rateLimiterConfig,
                           @Value("${app.gateway.rate-limit.exempt-paths:/actuator/health,/actuator/health/**,/api/benchmark/**}") String exemptPathsStr,
                           @Value("${app.gateway.rate-limit.header-limit:100}") String rateLimitHeaderValue,
                           @Value("${app.gateway.rate-limit.retry-after-seconds:60}") String retryAfterSeconds) {
        this.rateLimiterConfig = rateLimiterConfig;
        this.rateLimitExemptPaths = Arrays.asList(exemptPathsStr.split(","));
        this.rateLimitHeaderValue = rateLimitHeaderValue;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip rate limiting for exempt paths
        if (isExemptPath(path)) {
            return chain.filter(exchange);
        }

        // Get client identifier (user ID from JWT or IP address)
        String clientKey = getClientKey(exchange);

        boolean allowed = rateLimiterConfig.isAllowed(clientKey);

        if (allowed) {
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit", rateLimitHeaderValue);
            return chain.filter(exchange);
        } else {
            log.warn("Rate limit exceeded for client: {} on path: {}", clientKey, path);

            exchange.getResponse().getHeaders().add("X-Rate-Limit-Remaining", "0");
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit", rateLimitHeaderValue);
            exchange.getResponse().getHeaders().add("Retry-After", retryAfterSeconds);

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
    }

    private String getClientKey(ServerWebExchange exchange) {
        // Try to get user ID from JWT header set by JwtAuthenticationFilter
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // Fall back to IP address
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
        }

        return "ip:" + ip;
    }

    private boolean isExemptPath(String path) {
        return rateLimitExemptPaths.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -90; // Run after JWT filter
    }
}