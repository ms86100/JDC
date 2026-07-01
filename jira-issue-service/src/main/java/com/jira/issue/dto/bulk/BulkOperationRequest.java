package com.jira.issue.dto.bulk;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationRequest {
    @NotEmpty
    private List<UUID> issueIds;
    @NotNull
    private BulkOperationType operationType;
    private UUID projectId;
    private String newStatus;
    private UUID transitionId;
    private UUID assigneeId;
    private String priority;
    private UUID priorityId;
    private String labels;
    private UUID sprintId;
    private UUID targetProjectId;
}
