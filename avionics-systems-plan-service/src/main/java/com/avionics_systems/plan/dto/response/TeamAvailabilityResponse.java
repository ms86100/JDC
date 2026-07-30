package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAvailabilityResponse {
    private UUID id;
    private UUID teamId;
    private UUID userId;
    private LocalDate date;
    private BigDecimal hours;
    private String reason;
    private LocalDateTime createdAt;
}