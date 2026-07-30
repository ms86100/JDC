package com.avionics_systems.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveReportRequest {

    @NotBlank(message = "{validation.reportName.required}")
    private String name;

    private UUID projectId;

    @NotNull(message = "{validation.reportType.required}")
    private String reportType;

    private String reportConfig;
    private String filters;
    private String schedule;
    private Boolean isShared = false;
}