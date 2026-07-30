package com.avionics_systems.board.dto;

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
    private String timezone;
    private String workingDays;
    private String nonWorkingDates;
    private String timeTracking;
    private Boolean kanbanBacklogEnabled;
    private String subFilter;
    private Integer hideCompletedAfterDays;
    private Boolean useSimplifiedWorkflow;
}