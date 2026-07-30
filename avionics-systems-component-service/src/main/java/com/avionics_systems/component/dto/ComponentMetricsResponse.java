package com.avionics_systems.component.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentMetricsResponse {
    private UUID componentId;
    private String componentName;
    private LocalDate snapshotDate;
    private Integer totalIssues;
    private Integer openIssues;
    private Integer closedIssues;
    private Integer bugCount;
    private Integer storyCount;
    private Integer taskCount;
    private Double totalStoryPoints;
    private Double completedStoryPoints;
    private Double avgResolutionTimeHours;
}