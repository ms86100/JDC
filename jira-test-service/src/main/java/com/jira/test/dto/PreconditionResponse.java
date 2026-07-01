package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreconditionResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private String preconditionType;
    private String conditionScript;
    private String expectedResult;
    private String status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}