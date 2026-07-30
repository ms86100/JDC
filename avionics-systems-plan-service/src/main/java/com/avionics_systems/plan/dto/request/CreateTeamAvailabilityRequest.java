package com.avionics_systems.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeamAvailabilityRequest {
    private UUID userId;  // null means whole team
    private LocalDate date;
    private BigDecimal hours;  // 0 = full day off, 4 = half day
    private String reason;
}