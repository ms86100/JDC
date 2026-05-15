package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResponse {
    private String operationId;
    private BulkOperationType operationType;
    private Integer totalIssues;
    private Integer successCount;
    private Integer failedCount;
    private OperationStatus status;
    private List<BulkOperationResult> results;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}