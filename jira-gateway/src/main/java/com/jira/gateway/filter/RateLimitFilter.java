package com.jira.gateway.filter;

import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.jira.gateway.config.RateLimiterConfig;

import java.util.List;

/**
 * Rate Limiting Filter using Bucket4j Token Bucket algorithm.
 * Limits requests per user/IP to prevent abuse.
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterConfig rateLimiterConfig;

    private static final List<String> RATE_LIMIT_EXEMPT_PATHS = List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/api/benchmark/**"
    );

    public RateLimitFilter(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
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

        Bucket bucket = rateLimiterConfig.resolveBucket(clientKey);

        // Try to consume a token
        var probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit", "100");

            return chain.filter(exchange);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for client: {} on path: {}", clientKey, path);

            exchange.getResponse().getHeaders().add("X-Rate-Limit-Remaining", "0");
            exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit", "100");
            exchange.getResponse().getHeaders().add("Retry-After", "60");

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
        return RATE_LIMIT_EXEMPT_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -90; // Run after JWT filter
    }
}