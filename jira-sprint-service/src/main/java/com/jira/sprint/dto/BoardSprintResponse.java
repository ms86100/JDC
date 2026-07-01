package com.jira.sprint.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSprintResponse {
    private UUID id;
    private UUID boardId;
    private UUID sprintId;
    private String sprintName;
    private Integer sequence;
    private String state;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime completeDate;
}