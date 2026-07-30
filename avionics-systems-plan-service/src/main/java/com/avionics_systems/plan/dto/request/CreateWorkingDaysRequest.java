package com.avionics_systems.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkingDaysRequest {
    private String name;
    private Boolean monday = true;
    private Boolean tuesday = true;
    private Boolean wednesday = true;
    private Boolean thursday = true;
    private Boolean friday = true;
    private Boolean saturday = false;
    private Boolean sunday = false;
    private BigDecimal hoursPerDay = new BigDecimal("8.00");
    private Boolean isDefault = false;
}