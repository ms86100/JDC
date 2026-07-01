package com.jira.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardColumnResponse {
    private UUID id;
    private String name;
    private Integer sequence;
    private List<String> statusMapping;
    private List<String> labelValues;
    private Integer minWidth;
    private Integer maxWidth;
    private String color;
    private Integer maxIssues;
    private String constraintStatus;
}