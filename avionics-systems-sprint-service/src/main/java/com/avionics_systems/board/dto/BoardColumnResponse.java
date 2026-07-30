package com.avionics_systems.board.dto;

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
    private int sequence;
    private String statusCategory;
    private boolean isDone;
    private Integer maxIssues;
    private int currentIssues;
    private String color;
    private boolean isCollapsible;
    private boolean isHidden;
}