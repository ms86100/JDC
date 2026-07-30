package com.avionics_systems.board.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgileBoardResponse {
    private java.util.UUID id;
    private String name;
    private String description;
    private java.util.UUID projectId;
    private String boardType;
    private java.util.UUID filterId;
    private String jqlQuery;
    private boolean isDefault;
    private boolean allowAllIssues;
    private String cardLayout;
    private String estimationStatistic;
    private int daysOnBoard;
    private String timezone;
    private String workingDays;
    private String nonWorkingDates;
    private String timeTracking;
    private Boolean kanbanBacklogEnabled;
    private String subFilter;
    private Integer hideCompletedAfterDays;
    private Boolean useSimplifiedWorkflow;
    private java.time.LocalDateTime lastViewed;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}