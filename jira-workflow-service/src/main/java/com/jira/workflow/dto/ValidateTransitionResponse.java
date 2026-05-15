package com.jira.workflow.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTransitionResponse {

    private UUID fromStatusId;
    private UUID toStatusId;
    private boolean valid;
    private String message;
}