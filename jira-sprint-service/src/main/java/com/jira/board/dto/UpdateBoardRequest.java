package com.jira.board.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoardRequest {
    private String name;
    private String description;
    private String jqlQuery;
    private UUID filterId;
    private String cardLayout;
    private String estimationStatistic;
    private Integer daysOnBoard;
}