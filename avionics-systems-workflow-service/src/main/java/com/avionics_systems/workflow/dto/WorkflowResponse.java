package com.avionics_systems.workflow.dto;

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
    private Boolean isSystem;
    private Boolean isLocked;
    private UUID lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime publishedAt;
    private String type;
    private UUID defaultWorkflowId;
    private List<UUID> statusIds;
    private Integer statusCount;
    private Integer transitionCount;
    private Integer projectCount;
    private List<TransitionDetailResponse> transitions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Long version;
}