package com.avionics_systems.workflow.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAutomationRuleRequest {

    private String name;

    private String description;

    private UUID projectId;

    private String triggerType;

    private String triggerConfig;

    private String conditions;

    private String actions;

    private String branchType;

    private String branchLinkType;

    private String branchActions;
}
