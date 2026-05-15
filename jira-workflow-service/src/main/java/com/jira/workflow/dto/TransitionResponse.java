package com.jira.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionResponse {

    private UUID id;
    private UUID workflowId;
    private String name;
    private String description;
    private UUID fromStatusId;
    private UUID toStatusId;
    private Integer displayOrder;
    private String icon;
    private Boolean requiresApproval;
    private UUID approvalGroupId;
    private Boolean allowAssigneeOverride;
    private Boolean allowUnassign;
    private String fieldsRequired;
    private String fieldsUpdated;
    private String fieldsHidden;
    private String permissionCheck;
    private LocalDateTime createdAt;
}