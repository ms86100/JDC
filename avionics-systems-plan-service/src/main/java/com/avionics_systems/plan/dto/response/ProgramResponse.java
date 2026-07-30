package com.avionics_systems.plan.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private String ownerName;
    private String accessType;
    private Boolean isActive;
    private Integer planCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
