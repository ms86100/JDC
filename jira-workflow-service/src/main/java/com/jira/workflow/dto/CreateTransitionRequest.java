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

    @NotNull(message = "{validation.workflow.id.required}")
    private UUID workflowId;

    @NotBlank(message = "{validation.transition.name.required}")
    private String name;

    @NotNull(message = "{validation.transition.from.status.required}")
    private UUID fromStatusId;

    @NotNull(message = "{validation.transition.to.status.required}")
    private UUID toStatusId;

    @Builder.Default
    private boolean requiresApproval = false;
}