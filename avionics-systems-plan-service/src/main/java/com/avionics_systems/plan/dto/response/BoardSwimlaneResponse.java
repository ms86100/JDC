package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSwimlaneResponse {
    private UUID id;
    private String name;
    private String groupingField;
    private Boolean enabled;
    private Boolean collapsedByDefault;
    private Integer sequence;
}