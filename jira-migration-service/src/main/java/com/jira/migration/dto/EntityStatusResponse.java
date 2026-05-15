package com.jira.migration.dto;

import com.jira.migration.entity.EntityStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityStatusResponse {
    private UUID id;
    private UUID jobId;
    private String entityType;
    private String entityKey;
    private UUID entityId;
    private String status;
    private Integer processingOrder;
    private String errorCode;
    private String errorMessage;
    private Integer errorRow;
    private String errorField;
    private String errorContext;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer durationMs;
    private String validationErrors;
    private String warnings;

    public static EntityStatusResponse fromEntity(EntityStatus status) {
        return EntityStatusResponse.builder()
                .id(status.getId())
                .jobId(status.getJobId())
                .entityType(status.getEntityType())
                .entityKey(status.getEntityKey())
                .entityId(status.getEntityId())
                .status(status.getStatus())
                .processingOrder(status.getProcessingOrder())
                .errorCode(status.getErrorCode())
                .errorMessage(status.getErrorMessage())
                .errorRow(status.getErrorRow())
                .errorField(status.getErrorField())
                .errorContext(status.getErrorContext())
                .startedAt(status.getStartedAt())
                .completedAt(status.getCompletedAt())
                .durationMs(status.getDurationMs())
                .validationErrors(status.getValidationErrors())
                .warnings(status.getWarnings())
                .build();
    }
}