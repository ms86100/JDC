package com.jira.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSprintReportRequest {

    @NotNull(message = "{validation.sprintId.required}")
    private UUID sprintId;

    private String sprintName;
    private UUID projectId;
}