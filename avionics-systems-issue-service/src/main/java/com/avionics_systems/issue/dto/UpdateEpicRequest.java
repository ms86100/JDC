package com.avionics_systems.issue.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEpicRequest {
    private String name;
    private String summary;
    private String description;
    private String color;
    private String leadId;
    private String leadName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}