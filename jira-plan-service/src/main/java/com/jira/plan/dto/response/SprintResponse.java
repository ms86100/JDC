package com.jira.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintResponse {
    private UUID id;
    private UUID boardId;
    private String name;
    private String goal;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime completeDate;
    private String state;
    private Integer sequence;
    private Integer velocity;
    private Integer wipLimit;
    private Integer committedPoints;
    private Integer completedPoints;
    private int totalIssues;
    private int completedIssues;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}