package com.avionics_systems.migration.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter using token bucket algorithm.
 * Protects API endpoints from abuse and ensures fair resource allocation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RateLimitingFilter implements Filter {

    @Value("${rate-limiting.requests-per-second:100}")
    private int requestsPerSecond;

    @Value("${rate-limiting.burst-size:200}")
    private int burstSize;

    @Value("${rate-limiting.enabled:true}")
    private boolean enabled;

    // Per-URL rate limiting
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    // Global rate limiting
    private final TokenBucket globalBucket;

    public RateLimitingFilter() {
        this.globalBucket = new TokenBucket(500, 500); // Higher global limit
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip CORS preflight requests - add headers and return early
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, Accept, Origin, X-CSRF-Token, X-User-Id, Access-Control-Request-Method, Access-Control-Request-Headers");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");
            httpResponse.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        if (!enabled) {
            addCorsHeaders(httpResponse);
            chain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for health endpoints and WebSocket, but still add CORS headers
        String path = httpRequest.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/ws")) {
            addCorsHeaders(httpResponse);
            chain.doFilter(request, response);
            return;
        }

        String clientId = getClientIdentifier(httpRequest);
        String bucketKey = clientId + ":" + path;

        TokenBucket bucket = buckets.computeIfAbsent(bucketKey,
                k -> new TokenBucket(requestsPerSecond, burstSize));

        // Check global rate limit
        if (!globalBucket.tryAcquire()) {
            sendRateLimitResponse(httpResponse, "Global rate limit exceeded", 60);
            return;
        }

        // Check endpoint-specific rate limit
        if (!bucket.tryAcquire()) {
            log.warn("Rate limit exceeded for client: {}, path: {}", clientId, path);
            sendRateLimitResponse(httpResponse, "Rate limit exceeded for this endpoint", 30);
            return;
        }

        // Add CORS and rate limit headers
        addCorsHeaders(httpResponse);

        // Add rate limit headers
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerSecond));

        chain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Use X-Forwarded-For if behind proxy, otherwise use remote address
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, Accept, Origin, X-CSRF-Token, X-User-Id, Access-Control-Request-Method, Access-Control-Request-Headers");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message, int retryAfter)
            throws IOException {
        addCorsHeaders(response);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.getWriter().write(String.format(
                "{\"error\":\"rate_limit_exceeded\",\"message\":\"%s\",\"retryAfter\":%d}", message, retryAfter));
    }

    /**
     * Token bucket implementation for rate limiting.
     */
    private static class TokenBucket {
        private final int maxTokens;
        private final double refillRate; // tokens per second
        private final AtomicInteger tokens;
        private volatile Instant lastRefill;

        TokenBucket(int refillRate, int burstSize) {
            this.refillRate = refillRate;
            this.maxTokens = burstSize;
            this.tokens = new AtomicInteger(burstSize);
            this.lastRefill = Instant.now();
        }

        synchronized boolean tryAcquire() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        int getAvailableTokens() {
            refill();
            return tokens.get();
        }

        private void refill() {
            Instant now = Instant.now();
            long elapsedMs = Duration.between(lastRefill, now).toMillis();
            if (elapsedMs > 0) {
                double tokensToAdd = (elapsedMs / 1000.0) * refillRate;
                int currentTokens = tokens.get();
                int newTokens = Math.min(maxTokens, currentTokens + (int) tokensToAdd);
                tokens.set(newTokens);
                lastRefill = now;
            }
        }
    }

    /**
     * Clear rate limit buckets (for testing).
     */
    public void clearBuckets() {
        buckets.clear();
    }

    /**
     * Get current rate limit stats.
     */
    public Map<String, Integer> getRateLimitStats() {
        return Map.of(
                "global_available", globalBucket.getAvailableTokens(),
                "endpoint_count", buckets.size(),
                "requests_per_second", requestsPerSecond
        );
    }
}