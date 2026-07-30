package com.avionics_systems.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardSwimlaneRequest {
    private String name;
    private String groupingField;  // NONE, EPIC, ASSIGNEE, PROJECT, PRIORITY, LABEL
    private Integer sequence;
    private Boolean collapsedByDefault = false;
}