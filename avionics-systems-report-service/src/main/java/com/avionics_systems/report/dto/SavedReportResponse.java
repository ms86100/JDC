package com.avionics_systems.report.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedReportResponse {

    private UUID id;
    private String name;
    private UUID ownerId;
    private UUID projectId;
    private String reportType;
    private String reportConfig;
    private String filters;
    private String schedule;
    private LocalDateTime lastRunAt;
    private Boolean isShared;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}