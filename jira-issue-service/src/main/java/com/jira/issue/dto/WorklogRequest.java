package com.jira.issue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorklogRequest {
    @NotNull(message = "Issue ID is required")
    private UUID issueId;

    @NotNull(message = "Time spent is required")
    @Min(value = 1, message = "Time spent must be at least 1 second")
    private Long timeSpentSeconds;

    private String workDescription;
    private LocalDateTime startedAt;
    private UUID authorId;
}