package com.jira.issue.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEpicRequest {

    @NotBlank(message = "Epic name is required")
    private String name;

    private String summary;
    private String description;

    private String color = "#0052CC";

    private String leadId;
    private String leadName;
    private String status = "OPEN";

    private String linkedIssueId;

    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
}