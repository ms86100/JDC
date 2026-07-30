package com.avionics_systems.migration.exception;

public class RollbackException extends MigrationException {

    public RollbackException(String message) {
        super(message, "ROLLBACK_ERROR");
    }

    public RollbackException(String message, Throwable cause) {
        super(message, cause);
    }
}