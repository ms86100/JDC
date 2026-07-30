package com.avionics_systems.migration.service.clients.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating a new Sprint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSprintRequest {

    @NotBlank(message = "Sprint name is required")
    private String name;

    @NotBlank(message = "Project ID is required")
    private String projectId;

    private String goal;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer durationDays;
    private Integer capacity;
    private List<String> issueIds;
}