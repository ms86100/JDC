package com.avionics_systems.workflow.dto;

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