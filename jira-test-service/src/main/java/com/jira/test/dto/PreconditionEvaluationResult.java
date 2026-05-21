package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreconditionEvaluationResult {
    private UUID preconditionId;
    private String preconditionName;
    private boolean passed;
    private String reason;
    private LocalDateTime evaluatedAt;
    private List<String> errors;
}