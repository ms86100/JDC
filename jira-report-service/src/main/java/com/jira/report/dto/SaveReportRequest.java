package com.jira.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveReportRequest {

    @NotBlank(message = "Report name is required")
    private String name;

    private UUID projectId;

    @NotNull(message = "Report type is required")
    private String reportType;

    private String reportConfig;
    private String filters;
    private String schedule;
    private Boolean isShared = false;
}