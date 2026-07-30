package com.avionics_systems.plan.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {

    private UUID id;
    private UUID planId;
    private String name;
    private String description;
    private String status;
    private LocalDate targetDate;
    private Integer progress;
    private UUID parentGoalId;
    private List<String> linkedEpicIds;
    private String color;
    private UUID ownerUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<GoalResponse> children;
}
