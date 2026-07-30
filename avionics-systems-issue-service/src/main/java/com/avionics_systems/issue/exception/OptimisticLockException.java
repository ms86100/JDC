package com.avionics_systems.issue.exception;

/**
 * Exception thrown when optimistic locking detects a concurrent modification.
 * This prevents lost updates in distributed systems.
 */
public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException(String message) {
        super(message);
    }

    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}