package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VelocityResponse {
    private UUID projectId;
    private String projectName;
    private Integer currentVelocity;
    private Double averageVelocity;
    private Integer highestVelocity;
    private Integer lowestVelocity;
    private Integer totalSprints;
    private Integer completedSprints;
    private Double velocityTrend;
    private List<SprintVelocity> sprintVelocities;
}