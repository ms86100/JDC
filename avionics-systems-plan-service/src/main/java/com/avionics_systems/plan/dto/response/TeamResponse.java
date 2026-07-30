package com.avionics_systems.plan.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {

    private UUID id;
    private UUID planId;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer memberCount;
    private BigDecimal totalCapacity;
    private BigDecimal usedCapacity;
    private List<TeamMemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
