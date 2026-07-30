package com.avionics_systems.component.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentDetailResponse {
    // Base fields from ComponentResponse
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private UUID leadUserId;
    private String assigneeType;
    private UUID defaultAssignee;
    private Boolean archived;
    private String color;
    private String icon;
    private Integer sequence;
    private UUID createdBy;
    private UUID updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long issueCount;
    private Long openIssueCount;
    private Long closedIssueCount;
    private Long bugCount;
    private Long storyCount;

    // Extended fields
    private List<ComponentMetricsResponse> metricsHistory;
    private List<ComponentAssignmentRuleResponse> assignmentRules;
    private List<OwnershipTransferResponse> ownershipHistory;
}