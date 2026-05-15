package com.jira.migration.exception;

public class RollbackException extends MigrationException {

    public RollbackException(String message) {
        super(message, "ROLLBACK_ERROR");
    }

    public RollbackException(String message, Throwable cause) {
        super(message, cause);
    }
}