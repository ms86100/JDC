package com.jira.migration.service.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.jira.migration.security.MigrationRequestContext;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Abstract base class for all service clients providing common functionality:
 * - Circuit breaker support
 * - Retry logic
 * - Request/Response logging
 * - Auth token propagation
 * - Error mapping
 * - Timeout handling
 */
@Slf4j
public abstract class BaseServiceClient {

    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;
    protected final CircuitBreakerRegistry circuitBreakerRegistry;
    protected final String serviceName;
    protected final String baseUrl;

    private static final String AUTH_HEADER = "X-Auth-Token";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    protected BaseServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper,
                                  CircuitBreakerRegistry circuitBreakerRegistry,
                                  String serviceName, String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.serviceName = serviceName;
        this.baseUrl = baseUrl;
    }

    /**
     * Returns the circuit breaker name for this service client.
     */
    protected abstract String getCircuitBreakerName();

    /**
     * Returns the URL path prefix for this service (e.g., "/api/issues")
     */
    protected abstract String getServicePathPrefix();

    /**
     * Builds the full URL for an endpoint.
     */
    protected String buildUrl(String endpoint) {
        String base = baseUrl.replaceAll("/+$", "");
        String path = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return base + path;
    }

    /**
     * Gets the circuit breaker for this service.
     */
    protected CircuitBreaker getCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker(getCircuitBreakerName());
    }

    /**
     * Extracts auth token from the current request context.
     */
    protected String getAuthToken() {
        // Auth token will be handled by the migration service directly
        return null;
    }

    /**
     * Extracts correlation ID from request or generates a new one.
     */
    protected String getCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Creates HTTP headers with auth and correlation ID.
     */
    protected HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        String authToken = getAuthToken();
        if (authToken != null) {
            headers.set(AUTH_HEADER, authToken);
        }

        String correlationId = getCorrelationId();
        headers.set(CORRELATION_ID_HEADER, correlationId);

        UUID migrationUserId = MigrationRequestContext.getUserId();
        if (migrationUserId != null) {
            headers.set(USER_ID_HEADER, migrationUserId.toString());
        }

        return headers;
    }

    /**
     * Creates HTTP headers with custom content type.
     */
    protected HttpHeaders createHeaders(MediaType contentType) {
        HttpHeaders headers = createHeaders();
        headers.setContentType(contentType);
        return headers;
    }

    /**
     * Executes a GET request with circuit breaker and logging.
     */
    protected <T> T executeGet(String endpoint, Class<T> responseType) {
        return executeWithCircuitBreaker(() -> {
            String url = buildUrl(endpoint);
            log.debug("GET request to: {}", url);

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            long startTime = System.currentTimeMillis();
            try {
                ResponseEntity<T> response = restTemplate.exchange(
                        url, HttpMethod.GET, requestEntity, responseType);
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("GET {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
                return response.getBody();
            } catch (HttpClientErrorException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.warn("GET {} -> {} ({}ms): {}", url, e.getStatusCode(), elapsed, e.getMessage());
                throw ServiceClientException.clientError(serviceName, e.getStatusCode().value(),
                        e.getMessage(), endpoint, "GET");
            } catch (HttpServerErrorException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("GET {} -> {} ({}ms): {}", url, e.getStatusCode(), elapsed, e.getMessage());
                throw ServiceClientException.serverError(serviceName, e.getStatusCode().value(),
                        e.getMessage(), endpoint, "GET");
            } catch (RestClientException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("GET {} failed ({}ms): {}", url, elapsed, e.getMessage());
                throw ServiceClientException.connectionError(serviceName, endpoint, e);
            }
        });
    }

    /**
     * Executes a POST request with circuit breaker and logging.
     */
    protected <T, R> T executePost(String endpoint, R requestBody, Class<T> responseType) {
        return executeWithCircuitBreaker(() -> {
            String url = buildUrl(endpoint);
            log.debug("POST request to: {}", url);

            HttpHeaders headers = createHeaders();
            HttpEntity<R> requestEntity = new HttpEntity<>(requestBody, headers);

            long startTime = System.currentTimeMillis();
            try {
                ResponseEntity<T> response = restTemplate.exchange(
                        url, HttpMethod.POST, requestEntity, responseType);
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("POST {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
                return response.getBody();
            } catch (HttpClientErrorException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.warn("POST {} -> {} ({}ms): {}", url, e.getStatusCode(), elapsed, e.getMessage());
                throw ServiceClientException.clientError(serviceName, e.getStatusCode().value(),
                        e.getMessage(), endpoint, "POST");
            } catch (HttpServerErrorException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("POST {} -> {} ({}ms): {}", url, e.getStatusCode(), elapsed, e.getMessage());
                throw ServiceClientException.serverError(serviceName, e.getStatusCode().value(),
                        e.getMessage(), endpoint, "POST");
            } catch (RestClientException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("POST {} failed ({}ms): {}", url, elapsed, e.getMessage());
                throw ServiceClientException.connectionError(serviceName, endpoint, e);
            }
        });
    }

    /**
     * Executes a PUT request with circuit breaker and logging.
     */
    protected <T, R> T executePut(String endpoint, R requestBody, Class<T> responseType) {
        return executeWithCircuitBreaker(() -> {
            String url = buildUrl(endpoint);
            log.debug("PUT request to: {}", url);

            HttpHeaders headers = createHeaders();
            HttpEntity<R> requestEntity = new HttpEntity<>(requestBody, headers);

            long startTime = System.currentTimeMillis();
            try {
                ResponseEntity<T> response = restTemplate.exchange(
                        url, HttpMethod.PUT, requestEntity, responseType);
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("PUT {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
                return response.getBody();
            } catch (HttpClientErrorException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.warn("PUT {} -> {} ({}ms): {}", url, e.getStatusCode(), elapsed, e.getMessage());
                throw ServiceClientException.clientError(serviceName, e.getStatusCode().value(),
                        e.getMessage(), endpoint, "PUT");
            } catch (HttpServerErrorException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("PUT {} -> {} ({}ms): {}", url, e.getStatusCode(), elapsed, e.getMessage());
                throw ServiceClientException.serverError(serviceName, e.getStatusCode().value(),
                        e.getMessage(), endpoint, "PUT");
            } catch (RestClientException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("PUT {} failed ({}ms): {}", url, elapsed, e.getMessage());
                throw ServiceClientException.connectionError(serviceName, endpoint, e);
            }
        });
    }

    /**
     * Executes a DELETE request with circuit breaker and logging.
     */
    protected void executeDelete(String endpoint) {
        executeWithCircuitBreaker(() -> {
            String url = buildUrl(endpoint);
            log.debug("DELETE request to: {}", url);

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            long startTime = System.currentTimeMillis();
            try {
                ResponseEntity<Void> response = restTemplate.exchange(
                        url, HttpMethod.DELETE, requestEntity, Void.class);
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("DELETE {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
                return null;
            } catch (HttpClientErrorException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.warn("DELETE {} -> {} ({}ms): {}", url, e.getStatusCode(), elapsed, e.getMessage());
                throw ServiceClientException.clientError(serviceName, e.getStatusCode().value(),
                        e.getMessage(), endpoint, "DELETE");
            } catch (HttpServerErrorException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("DELETE {} -> {} ({}ms): {}", url, e.getStatusCode(), elapsed, e.getMessage());
                throw ServiceClientException.serverError(serviceName, e.getStatusCode().value(),
                        e.getMessage(), endpoint, "DELETE");
            } catch (RestClientException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("DELETE {} failed ({}ms): {}", url, elapsed, e.getMessage());
                throw ServiceClientException.connectionError(serviceName, endpoint, e);
            }
        });
    }

    /**
     * Executes a request with circuit breaker protection.
     */
    protected <T> T executeWithCircuitBreaker(Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = getCircuitBreaker();
        return circuitBreaker.executeSupplier(supplier);
    }

    /**
     * Executes a request with retry logic and exponential backoff.
     * Retries on retryable exceptions (connection errors, 5xx errors).
     */
    protected <T> T executeWithRetry(Supplier<T> supplier, int maxRetries, long baseDelayMs) {
        int attempt = 0;
        long delay = baseDelayMs;

        while (true) {
            try {
                attempt++;
                return supplier.get();
            } catch (ServiceClientException e) {
                if (!e.isRetryable() || attempt >= maxRetries) {
                    log.error("Request failed after {} attempts: {}", attempt, e.getMessage());
                    throw e;
                }
                log.warn("Request failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt, maxRetries, delay, e.getMessage());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                delay *= 2; // Exponential backoff
            }
        }
    }

    /**
     * Gets an optional response, returning empty on circuit breaker open.
     */
    protected <T> Optional<T> executeGetOptional(String endpoint, Class<T> responseType) {
        try {
            return Optional.ofNullable(executeGet(endpoint, responseType));
        } catch (ServiceClientException e) {
            log.warn("Optional GET failed for {}: {}", endpoint, e.getMessage());
            return Optional.empty();
        }
    }
}