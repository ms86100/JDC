package com.avionics_systems.sprint.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateColumnRequest {
    private String name;
    private String statusIds;  // JSON array
    private String statusCategory;
    private Boolean isDone;
    private Integer maxIssues;
    private String color;
    private Boolean isCollapsible;
    private Boolean isHidden;
}