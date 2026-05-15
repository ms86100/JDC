package com.jira.board.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelocityResponse {
    private double averageVelocity;
    private List<VelocityPoint> velocityPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityPoint {
        private String sprintName;
        private int completed;
        private int planned;
    }
}