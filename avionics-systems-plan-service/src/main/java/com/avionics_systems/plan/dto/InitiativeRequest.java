package com.avionics_systems.plan.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeRequest {

    private String name;
    private String description;
    private UUID ownerId;
    private UUID programId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate targetDate;
    private String color;
    private String avatarUrl;
}