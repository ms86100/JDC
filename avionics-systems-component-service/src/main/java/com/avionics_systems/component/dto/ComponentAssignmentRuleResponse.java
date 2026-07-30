package com.avionics_systems.component.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentAssignmentRuleResponse {
    private UUID id;
    private UUID componentId;
    private String ruleType;
    private UUID issueTypeId;
    private UUID priorityId;
    private String assigneeType;
    private UUID assigneeId;
    private Boolean isActive;
}