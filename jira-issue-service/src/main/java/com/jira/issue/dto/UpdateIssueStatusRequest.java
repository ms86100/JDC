package com.jira.issue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueStatusRequest {

    @NotNull(message = "Status ID is required")
    private UUID statusId;
}