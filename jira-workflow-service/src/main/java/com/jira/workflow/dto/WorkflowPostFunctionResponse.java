package com.jira.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for workflow post-function data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPostFunctionResponse {

    private UUID id;
    private UUID transitionId;
    private String postFunctionType;
    private String functionData;
    private Integer sequence;
    private Boolean enabled;
    private Boolean continueOnError;
    private Boolean async;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}