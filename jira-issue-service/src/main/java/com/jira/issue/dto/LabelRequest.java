package com.jira.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelRequest {
    @NotNull(message = "Issue ID is required")
    private UUID issueId;

    @NotBlank(message = "Label name is required")
    private String name;
}