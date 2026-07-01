package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransitionRequest {

    @NotNull(message = "Workflow ID is required")
    private UUID workflowId;

    @NotBlank(message = "Transition name is required")
    private String name;

    @NotNull(message = "From status ID is required")
    private UUID fromStatusId;

    @NotNull(message = "To status ID is required")
    private UUID toStatusId;

    @Builder.Default
    private boolean requiresApproval = false;
}