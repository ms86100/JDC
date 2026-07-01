package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepTemplateResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private String category;

    // Template steps
    private List<SharedStepDto> steps;
    private Integer stepCount;

    // Metadata
    private List<String> tags;
    private List<String> labels;
    private String instructions;

    // Variables
    private List<TemplateVariable> variables;

    // Usage
    private Integer usageCount; // How many shared steps created from this template

    // Audit
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}