package com.jira.board.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardRequest {
    private String name;
    private String description;
    private UUID projectId;
    private String boardType;
    private UUID filterId;
    private String jqlQuery;
    private boolean isDefault;
    private boolean allowAllIssues;
    private String cardLayout;
    private String estimationStatistic;
    private Integer daysOnBoard;
}