package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintBurndownResponse {
    private UUID sprintId;
    private String sprintName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalIssues;
    private int completedIssues;
    private int totalPoints;
    private int completedPoints;
    private List<BurndownPoint> burndownPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurndownPoint {
        private LocalDate date;
        private int remainingIssues;
        private int completedIssues;
        private int remainingPoints;
        private int idealRemaining;
    }
}