package com.jira.migration.exception;

public class BatchProcessingException extends RuntimeException {

    private final String batchId;
    private final int batchNumber;
    private final String entityType;

    public BatchProcessingException(String message) {
        super(message);
        this.batchId = null;
        this.batchNumber = -1;
        this.entityType = null;
    }

    public BatchProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.batchId = null;
        this.batchNumber = -1;
        this.entityType = null;
    }

    public BatchProcessingException(String message, String batchId, int batchNumber, String entityType) {
        super(message);
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.entityType = entityType;
    }

    public BatchProcessingException(String message, Throwable cause, String batchId, int batchNumber, String entityType) {
        super(message, cause);
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.entityType = entityType;
    }

    public String getBatchId() {
        return batchId;
    }

    public int getBatchNumber() {
        return batchNumber;
    }

    public String getEntityType() {
        return entityType;
    }

    @Override
    public String toString() {
        return String.format("BatchProcessingException{message='%s', batchId='%s', batchNumber=%d, entityType='%s'}",
                getMessage(), batchId, batchNumber, entityType);
    }
}