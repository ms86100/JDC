package com.avionics_systems.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private List<String> fieldsRequired;
    private List<Map<String, Object>> fieldsUpdated;
    private List<String> fieldsHidden;
    private String permissionCheck;
    private LocalDateTime createdAt;
}