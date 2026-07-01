package com.jira.sprint.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardColumnResponse {
    private UUID id;
    private UUID boardId;
    private String name;
    private Integer sequence;
    private String statusIds;
    private String statusCategory;
    private Boolean isDone;
    private Integer maxIssues;
    private String color;
    private Boolean isCollapsible;
    private Boolean isHidden;
}