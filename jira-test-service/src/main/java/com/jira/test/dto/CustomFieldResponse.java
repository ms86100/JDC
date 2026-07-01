package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String fieldKey;
    private String fieldType;
    private String description;
    private Object options;
    private Object defaultValue;
    private Object validationRules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}