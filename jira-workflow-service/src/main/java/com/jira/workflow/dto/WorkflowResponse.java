package com.jira.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private Boolean isDefault;
    private Boolean isDraft;
    private Boolean isActive;
    private List<UUID> statusIds;
    private List<TransitionDetailResponse> transitions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}