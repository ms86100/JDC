package com.jira.migration.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultResponse {
    private UUID jobId;
    private String jobStatus;
    private Integer totalEntities;
    private Integer processedEntities;
    private Integer failedEntities;
    private Integer successCount;
    private Integer warningCount;
    private List<EntityError> errors;
    private List<EntityWarning> warnings;
    private String errorSummary;
    private String resultMetadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityError {
        private String entityType;
        private String entityKey;
        private Integer row;
        private String field;
        private String errorCode;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityWarning {
        private String entityType;
        private String entityKey;
        private Integer row;
        private String field;
        private String warningMessage;
    }
}