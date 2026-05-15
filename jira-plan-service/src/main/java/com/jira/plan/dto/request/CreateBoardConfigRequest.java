package com.jira.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardConfigRequest {
    private String name;
    private String boardType = "SCRUM";  // SCRUM, KANBAN
    private String columnConfigMode = "DEFAULT";  // DEFAULT, LABEL, STATUS
    private String constraintSource;
    private String cardLayoutMode = "COMPACT";  // COMPACT, FULL
    private String defaultSwimlane = "NONE";  // NONE, EPIC, ASSIGNEE, PROJECT, PRIORITY
    private Boolean isEnabled = true;
}