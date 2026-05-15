package com.jira.sprint.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationRequest {
    private List<UUID> issueIds;
    private BulkOperationType operationType;

    // For status change
    private String newStatus;

    // For field updates
    private String assigneeId;
    private String priority;
    private String labels;
    private String sprintId;

    // For clone
    private UUID targetProjectId;
    private Boolean keepLinks;
    private Boolean keepAttachments;
}