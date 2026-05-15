package com.jira.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingDaysResponse {
    private UUID id;
    private String name;
    private Boolean monday;
    private Boolean tuesday;
    private Boolean wednesday;
    private Boolean thursday;
    private Boolean friday;
    private Boolean saturday;
    private Boolean sunday;
    private BigDecimal hoursPerDay;
    private Boolean isDefault;
    private int workingDaysPerWeek;
    private List<NonWorkingDayResponse> holidays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}