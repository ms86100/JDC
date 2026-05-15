package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BurndownDataPoint {
    private LocalDate date;
    private Double remainingPoints;
    private Double idealPoints;
    private Integer totalIssues;
    private Integer completedIssues;
    private Integer addedIssues;
    private Integer removedIssues;
}