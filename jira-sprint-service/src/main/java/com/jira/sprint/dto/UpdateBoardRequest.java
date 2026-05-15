package com.jira.sprint.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoardRequest {
    private String name;
    private String description;
    private UUID filterId;
    private String jqlQuery;
    private String columnConfig;
    private String cardLayout;
    private String estimationStatistic;
    private Integer daysOnBoard;
    private String backlogColumn;
}