package com.avionics_systems.migration.exception;

public class TransactionManagementException extends RuntimeException {

    private final String transactionId;
    private final String operation;
    private final boolean rollbackPerformed;

    public TransactionManagementException(String message) {
        super(message);
        this.transactionId = null;
        this.operation = null;
        this.rollbackPerformed = false;
    }

    public TransactionManagementException(String message, Throwable cause) {
        super(message, cause);
        this.transactionId = null;
        this.operation = null;
        this.rollbackPerformed = false;
    }

    public TransactionManagementException(String message, String transactionId, String operation) {
        super(message);
        this.transactionId = transactionId;
        this.operation = operation;
        this.rollbackPerformed = false;
    }

    public TransactionManagementException(String message, Throwable cause, String transactionId, String operation, boolean rollbackPerformed) {
        super(message, cause);
        this.transactionId = transactionId;
        this.operation = operation;
        this.rollbackPerformed = rollbackPerformed;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOperation() {
        return operation;
    }

    public boolean isRollbackPerformed() {
        return rollbackPerformed;
    }

    @Override
    public String toString() {
        return String.format("TransactionManagementException{message='%s', transactionId='%s', operation='%s', rollbackPerformed=%s}",
                getMessage(), transactionId, operation, rollbackPerformed);
    }
}