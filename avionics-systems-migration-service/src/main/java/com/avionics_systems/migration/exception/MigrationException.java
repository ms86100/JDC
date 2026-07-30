package com.avionics_systems.migration.exception;

public class MigrationException extends RuntimeException {
    private final String errorCode;
    private final String field;

    public MigrationException(String message) {
        super(message);
        this.errorCode = "MIGRATION_ERROR";
        this.field = null;
    }

    public MigrationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.field = null;
    }

    public MigrationException(String message, String errorCode, String field) {
        super(message);
        this.errorCode = errorCode;
        this.field = field;
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "MIGRATION_ERROR";
        this.field = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getField() {
        return field;
    }
}