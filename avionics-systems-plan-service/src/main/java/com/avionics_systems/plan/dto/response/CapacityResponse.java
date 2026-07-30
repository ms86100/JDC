package com.avionics_systems.plan.dto.response;

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
public class CapacityResponse {
    private UUID teamId;
    private LocalDate startDate;
    private LocalDate endDate;
    private long workingDays;
    private BigDecimal totalCapacityHours;
    private BigDecimal totalTimeOffHours;
    private BigDecimal netCapacityHours;
    private int memberCount;
}