package com.jira.plan.dto.request;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlanRequest {

    private String name;
    private String description;
    private UUID ownerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> settings;
    private Boolean isActive;
}