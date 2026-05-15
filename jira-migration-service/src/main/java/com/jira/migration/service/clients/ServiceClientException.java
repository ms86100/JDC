package com.jira.migration.service.clients;

import lombok.Getter;

/**
 * Custom exception for service client errors with support for retryable errors
 * and HTTP status code tracking.
 */
@Getter
public class ServiceClientException extends RuntimeException {

    private final String serviceName;
    private final int statusCode;
    private final boolean retryable;
    private final String endpoint;
    private final String requestMethod;

    public ServiceClientException(String message, String serviceName, int statusCode,
                                   boolean retryable, String endpoint, String requestMethod) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.endpoint = endpoint;
        this.requestMethod = requestMethod;
    }

    public ServiceClientException(String message, String serviceName, int statusCode,
                                   boolean retryable, String endpoint, String requestMethod, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.endpoint = endpoint;
        this.requestMethod = requestMethod;
    }

    public ServiceClientException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = "unknown";
        this.statusCode = -1;
        this.retryable = false;
        this.endpoint = "unknown";
        this.requestMethod = "unknown";
    }

    public ServiceClientException(String message, String serviceName) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = -1;
        this.retryable = false;
        this.endpoint = "unknown";
        this.requestMethod = "unknown";
    }

    /**
     * Creates an exception for HTTP 4xx errors (client errors - generally not retryable)
     */
    public static ServiceClientException clientError(String serviceName, int statusCode,
                                                      String message, String endpoint, String method) {
        return new ServiceClientException(
            String.format("Client error from %s: %s (status=%d, endpoint=%s, method=%s)",
                serviceName, message, statusCode, endpoint, method),
            serviceName, statusCode, false, endpoint, method
        );
    }

    /**
     * Creates an exception for HTTP 5xx errors (server errors - generally retryable)
     */
    public static ServiceClientException serverError(String serviceName, int statusCode,
                                                      String message, String endpoint, String method) {
        return new ServiceClientException(
            String.format("Server error from %s: %s (status=%d, endpoint=%s, method=%s)",
                serviceName, message, statusCode, endpoint, method),
            serviceName, statusCode, true, endpoint, method
        );
    }

    /**
     * Creates an exception for connection/timeout errors (retryable)
     */
    public static ServiceClientException connectionError(String serviceName, String endpoint,
                                                          Throwable cause) {
        return new ServiceClientException(
            String.format("Connection error to %s (endpoint=%s): %s",
                serviceName, endpoint, cause.getMessage()),
            serviceName, -1, true, endpoint, "UNKNOWN", cause
        );
    }

    @Override
    public String toString() {
        return String.format("ServiceClientException{service='%s', status=%d, retryable=%s, endpoint='%s', method='%s', message='%s'}",
            serviceName, statusCode, retryable, endpoint, requestMethod, getMessage());
    }
}
