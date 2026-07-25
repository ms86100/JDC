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

    @NotNull(message = "{validation.transition.id.required}")
    private UUID transitionId;

    @NotBlank(message = "{validation.postfunction.type.required}")
    private String functionType;

    private String functionData;
    private Integer sequence;
    private Boolean async;
    private Boolean failOnError;
}