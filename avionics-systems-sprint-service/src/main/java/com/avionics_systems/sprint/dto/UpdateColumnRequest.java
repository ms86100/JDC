package com.avionics_systems.sprint.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateColumnRequest {
    private String name;
    private String statusIds;
    private String statusCategory;
    private Boolean isDone;
    private Integer maxIssues;
    private String color;
    private Boolean isCollapsible;
    private Boolean isHidden;
}