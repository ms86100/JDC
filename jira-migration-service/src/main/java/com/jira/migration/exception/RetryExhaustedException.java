package com.jira.migration.exception;

public class RetryExhaustedException extends RuntimeException {

    private final String operationName;
    private final int attemptsMade;
    private final int maxAttempts;
    private final String lastError;

    public RetryExhaustedException(String operationName, int attemptsMade, int maxAttempts, String lastError) {
        super(String.format("Retry exhausted for operation '%s' after %d attempts (max: %d). Last error: %s",
                operationName, attemptsMade, maxAttempts, lastError));
        this.operationName = operationName;
        this.attemptsMade = attemptsMade;
        this.maxAttempts = maxAttempts;
        this.lastError = lastError;
    }

    public RetryExhaustedException(String operationName, int attemptsMade, int maxAttempts, Throwable lastError) {
        this(operationName, attemptsMade, maxAttempts, lastError != null ? lastError.getMessage() : "Unknown error");
    }

    public String getOperationName() {
        return operationName;
    }

    public int getAttemptsMade() {
        return attemptsMade;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public double getAttemptRatio() {
        return (double) attemptsMade / maxAttempts;
    }
}