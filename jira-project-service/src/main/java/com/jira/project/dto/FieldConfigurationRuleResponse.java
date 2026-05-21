package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfigurationRuleResponse {
    private String fieldKey;
    private UUID issueTypeId;
    private boolean required;
    private boolean visible;
    private boolean hidden;
}
