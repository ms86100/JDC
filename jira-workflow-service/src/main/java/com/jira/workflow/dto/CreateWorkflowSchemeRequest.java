package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowSchemeRequest {
    @NotBlank(message = "Scheme name is required")
    @Size(max = 200, message = "Scheme name must not exceed 200 characters")
    private String name;

    private String description;

    private Boolean isDefault = false;

    private UUID defaultWorkflowId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UpdateWorkflowSchemeRequest {
    @Size(max = 200, message = "Scheme name must not exceed 200 characters")
    private String name;

    private String description;

    private Boolean isDefault;

    private UUID defaultWorkflowId;
}