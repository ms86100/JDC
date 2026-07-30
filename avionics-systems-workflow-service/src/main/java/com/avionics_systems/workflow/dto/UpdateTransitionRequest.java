package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransitionRequest {

    private String name;

    private String description;

    private String icon;

    private String type;

    private String triggerType;

    private Integer displayOrder;

    private Boolean requiresApproval;

    private UUID approvalGroupId;

    private Boolean allowAssigneeOverride;

    private Boolean allowUnassign;

    private List<String> fieldsRequired;

    private List<String> fieldsHidden;

    private String permissionCheck;

    private List<String> userGroupIds;

    private Boolean allowLoop;

    private Integer maxLoopCount;

    private UUID screenId;
}