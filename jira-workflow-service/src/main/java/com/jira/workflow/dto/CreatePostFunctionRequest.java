package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostFunctionRequest {

    @NotNull(message = "Transition ID is required")
    private UUID transitionId;

    @NotBlank(message = "Function type is required")
    private String functionType;

    private String functionData;
    private Integer sequence;
    private Boolean async;
    private Boolean failOnError;
}