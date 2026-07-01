package com.jira.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardColumnRequest {
    private String name;
    private Integer sequence;
    private List<String> statusMapping;
    private List<String> labelValues;
    private Integer minWidth = 100;
    private Integer maxWidth = 600;
    private String color;
    private Integer maxIssues;  // WIP limit
    private String constraintStatus;
}