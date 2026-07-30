package com.avionics_systems.sprint.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BurndownResponse {
    private UUID sprintId;
    private String sprintName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalPoints;
    private Integer completedPoints;
    private Integer remainingPoints;
    private Integer totalIssues;
    private Integer completedIssues;
    private Double completionRate;
    private List<BurndownDataPoint> dailyData;
}