package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreconditionRequest {
    private String name;
    private String description;
    private String preconditionType;
    private String conditionScript;
    private String expectedResult;
    private String status;
}