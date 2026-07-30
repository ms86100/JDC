package com.avionics_systems.migration.exception;

public class DlqOperationException extends RuntimeException {

    private final String dlqId;
    private final String operation;
    private final String reason;

    public DlqOperationException(String message) {
        super(message);
        this.dlqId = null;
        this.operation = null;
        this.reason = null;
    }

    public DlqOperationException(String message, String dlqId, String operation, String reason) {
        super(message);
        this.dlqId = dlqId;
        this.operation = operation;
        this.reason = reason;
    }

    public DlqOperationException(String message, Throwable cause) {
        super(message, cause);
        this.dlqId = null;
        this.operation = null;
        this.reason = null;
    }

    public DlqOperationException(String message, Throwable cause, String dlqId, String operation, String reason) {
        super(message, cause);
        this.dlqId = dlqId;
        this.operation = operation;
        this.reason = reason;
    }

    public String getDlqId() {
        return dlqId;
    }

    public String getOperation() {
        return operation;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("DlqOperationException{message='%s', dlqId='%s', operation='%s', reason='%s'}",
                getMessage(), dlqId, operation, reason);
    }
}