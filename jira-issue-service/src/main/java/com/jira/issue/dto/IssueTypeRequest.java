package com.jira.issue.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTypeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Issue type key is required")
    private String issueTypeKey;

    private boolean isSubtask;

    private String icon;

    private String color;

    private int sequence;
}