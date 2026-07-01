package com.jira.issue.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResponse {
    private String operationId;
    private BulkOperationType operationType;
    private int totalIssues;
    private int successCount;
    private int failedCount;
    private String status;
    @Builder.Default
    private List<BulkOperationResultItem> results = new ArrayList<>();
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
