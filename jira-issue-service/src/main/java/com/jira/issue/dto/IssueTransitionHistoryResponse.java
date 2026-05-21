package com.jira.issue.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTransitionHistoryResponse {

    private UUID id;
    private UUID issueId;
    private UUID projectId;
    private UUID workflowId;
    private UUID transitionId;
    private String transitionName;
    private UUID fromStatusId;
    private UUID toStatusId;
    private UUID userId;
    private String comment;
    private Boolean success;
    private String errorMessage;
    private OffsetDateTime executedAt;
}
