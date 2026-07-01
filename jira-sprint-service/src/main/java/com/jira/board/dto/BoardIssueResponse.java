package com.jira.board.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardIssueResponse {
    private UUID id;
    private String issueKey;
    private String title;
    private String status;
    private String priority;
    private String issueType;
    private UUID assigneeId;
    private String assigneeName;
    private UUID reporterId;
    private UUID epicId;
    private String epicName;
    private String epicColor;
    private Integer storyPoints;
    private List<String> labels;
    private LocalDateTime created;
    private LocalDateTime updated;
    private UUID sprintId;
    private String sprintName;
    private String dueDate;
    private String rank;
}