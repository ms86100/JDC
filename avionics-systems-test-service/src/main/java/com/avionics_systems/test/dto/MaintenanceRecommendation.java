package com.avionics_systems.test.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecommendation {

    private String category;
    private String priority;
    private String title;
    private String description;
    private String actionType;
    private Object targetId;
    private String targetName;
    private Integer estimatedImpact;
}