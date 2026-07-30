package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a test plan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestPlanRequest {

    private String name;
    private String description;
    private String testType;
    private List<String> labels;
    private LocalDate startDate;
    private LocalDate endDate;
    private String targetVersion;
    private String environment;
    private UUID ownerId;
}