package com.avionics_systems.migration.exception;

public class ValidationException extends MigrationException {
    private final Integer row;
    private final Object invalidValue;

    public ValidationException(String message, String errorCode, String field) {
        super(message, errorCode, field);
        this.row = null;
        this.invalidValue = null;
    }

    public ValidationException(String message, String errorCode, String field, Integer row) {
        super(message, errorCode, field);
        this.row = row;
        this.invalidValue = null;
    }

    public ValidationException(String message, String errorCode, String field, Integer row, Object invalidValue) {
        super(message, errorCode, field);
        this.row = row;
        this.invalidValue = invalidValue;
    }

    public Integer getRow() {
        return row;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }
}