package com.avionics_systems.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDraftResponse {
    private UUID id;
    private UUID workflowId;
    private String workflowName;
    private String name;
    private String description;
    private String draftData;
    private Integer parentVersion;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDraftOfPublished;
    private String draftStatus;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreateDraftRequest {
    private UUID workflowId;
    private UUID userId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PublishDraftRequest {
    private UUID draftId;
    private UUID userId;
    private String changeDescription;
}