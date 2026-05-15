package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintVelocity {
    private UUID sprintId;
    private String sprintName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer committedPoints;
    private Integer completedPoints;
    private Double reliability;
    private Boolean isCompleted;
}