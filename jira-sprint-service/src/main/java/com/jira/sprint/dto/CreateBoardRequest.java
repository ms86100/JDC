package com.jira.sprint.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardRequest {
    private UUID projectId;
    private String name;
    private String description;
    private String boardType;  // SCRUM, KANBAN, BADGE
    private UUID filterId;
    private String jqlQuery;
    private boolean isDefault;
    private Boolean allowAllIssues;
    private boolean isCommunity;
    private String location;
    private Boolean canManage;
    private String columnConfig;
    private String rankingConfig;
    private String cardLayout;
    private String estimationStatistic;
    private Integer daysOnBoard;
    private String backlogColumn;
}