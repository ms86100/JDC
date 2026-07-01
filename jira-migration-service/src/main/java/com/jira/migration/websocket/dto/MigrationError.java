package com.jira.migration.websocket.dto;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationError {
    private String jobId;
    private String errorCode;
    private String errorMessage;
    private String entityType;
    private String entityKey;
    private Integer row;
    private String field;
    private Map<String, Object> context;
    private Instant timestamp;
    private String severity; // ERROR, WARNING, CRITICAL

    public static MigrationError fromEntityStatus(com.jira.migration.entity.EntityStatus status) {
        return MigrationError.builder()
                .jobId(status.getJobId().toString())
                .errorCode(status.getErrorCode())
                .errorMessage(status.getErrorMessage())
                .entityType(status.getEntityType())
                .entityKey(status.getEntityKey())
                .row(status.getErrorRow())
                .field(status.getErrorField())
                .timestamp(Instant.now())
                .severity("ERROR")
                .build();
    }
}