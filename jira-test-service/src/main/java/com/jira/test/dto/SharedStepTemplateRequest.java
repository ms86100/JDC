package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepTemplateRequest {

    private UUID projectId;

    private String name;
    private String description;
    private String category;

    // Template steps - can include placeholders
    private List<SharedStepDto> steps;

    // Metadata
    private List<String> tags;
    private List<String> labels;
    private String instructions;

    // Pre-defined variables for the template
    private List<TemplateVariable> variables;
}