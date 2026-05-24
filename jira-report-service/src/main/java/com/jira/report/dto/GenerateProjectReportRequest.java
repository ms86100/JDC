package com.jira.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateProjectReportRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private String projectKey;
    private String reportType = "SUMMARY";
}