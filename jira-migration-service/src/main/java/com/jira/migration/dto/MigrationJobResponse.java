package com.jira.migration.dto;

import com.jira.migration.entity.MigrationJob;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationJobResponse {
    private UUID id;
    private String jobType;
    private String jobStatus;
    private String importSource;
    private Integer totalEntities;
    private Integer processedEntities;
    private Integer failedEntities;
    private Double progressPercentage;
    private String config;
    private String options;
    private UUID initiatedBy;
    private LocalDateTime initiatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private String errorDetails;
    private UUID sourceProjectId;
    private UUID targetProjectId;
    private String filePath;
    private Boolean canRollback;
    private UUID rollbackJobId;
    private String resultMetadata;

    public static MigrationJobResponse fromEntity(MigrationJob job) {
        return MigrationJobResponse.builder()
                .id(job.getId())
                .jobType(job.getJobType())
                .jobStatus(job.getJobStatus())
                .importSource(job.getImportSource())
                .totalEntities(job.getTotalEntities())
                .processedEntities(job.getProcessedEntities())
                .failedEntities(job.getFailedEntities())
                .progressPercentage(job.getProgressPercentage())
                .config(job.getConfig())
                .options(job.getOptions())
                .initiatedBy(job.getInitiatedBy())
                .initiatedAt(job.getInitiatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .errorMessage(job.getErrorMessage())
                .errorDetails(job.getErrorDetails())
                .sourceProjectId(job.getSourceProjectId())
                .targetProjectId(job.getTargetProjectId())
                .filePath(job.getFilePath())
                .canRollback(job.getCanRollback())
                .rollbackJobId(job.getRollbackJobId())
                .resultMetadata(job.getResultMetadata())
                .build();
    }
}