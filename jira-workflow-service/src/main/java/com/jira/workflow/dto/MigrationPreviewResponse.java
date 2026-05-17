package com.jira.workflow.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MigrationPreviewResponse {
    private UUID workflowId;
    private UUID oldStatusId;
    private String oldStatusName;
    private UUID newStatusId;
    private String newStatusName;
    private Integer issueCount;
    private List<IssuePreview> issues;
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
class IssuePreview {
    private UUID issueId;
    private String issueKey;
    private String summary;
    private UUID currentStatusId;
}