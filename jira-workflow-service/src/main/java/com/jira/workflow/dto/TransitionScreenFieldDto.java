package com.jira.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionScreenFieldDto {
    private String fieldId;
    private String fieldName;
    private String fieldType;
    private boolean required;
    private String defaultValue;
}
