package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomFieldRequest {

    private String name;

    private String description;

    private String options;

    private String defaultValue;

    private String validationRules;
}