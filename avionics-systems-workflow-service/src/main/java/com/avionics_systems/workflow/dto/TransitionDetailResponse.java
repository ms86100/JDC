package com.avionics_systems.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionDetailResponse {

    private UUID id;
    private UUID workflowId;
    private String name;
    private String description;
    private UUID fromStatusId;
    private UUID toStatusId;
    private String fromStatusName;
    private String toStatusName;
    private String fromStatusCategory;
    private String toStatusCategory;
    private String fromStatusColor;
    private String toStatusColor;
    private Integer displayOrder;
    private String icon;
    private Boolean requiresApproval;
    private UUID approvalGroupId;
    private Boolean allowAssigneeOverride;
    private Boolean allowUnassign;
    private List<String> fieldsRequired;
    private List<Map<String, Object>> fieldsUpdated;
    private List<String> fieldsHidden;
    private Boolean fieldsAutoSubmit;
    private String permissionCheck;
    private List<String> userGroupIds;
    private Boolean remoteLinkTransition;
    private String remoteLinkDirection;
    private String remoteLinkIssueLinkType;
    private Boolean allowLoop;
    private Integer maxLoopCount;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<ConditionResponse> conditions = new ArrayList<>();

    @Builder.Default
    private List<ValidatorResponse> validators = new ArrayList<>();

    @Builder.Default
    private List<PostFunctionResponse> postFunctions = new ArrayList<>();
}