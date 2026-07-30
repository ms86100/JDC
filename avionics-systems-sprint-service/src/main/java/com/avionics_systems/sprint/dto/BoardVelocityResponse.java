package com.avionics_systems.sprint.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardVelocityResponse {
    @Builder.Default
    private List<VelocityPoint> velocityPoints = new ArrayList<>();
    private double averageVelocity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityPoint {
        private UUID sprintId;
        private String sprintName;
        private int completedIssues;
        private int plannedIssues;
    }
}