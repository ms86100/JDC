package com.avionics_systems.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeTrackingReportRequest {

    @NotNull(message = "{validation.startDate.required}")
    private LocalDateTime startDate;

    @NotNull(message = "{validation.endDate.required}")
    private LocalDateTime endDate;

    private UUID projectId;
    private UUID issueId;
    private UUID userId;

    private String reportType = "USER";
}