package com.jira.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardDetailFieldRequest {
    private String fieldKey;  // summary, priority, assignee, etc.
    private String fieldLabel;
    private Integer sequence;
    private Boolean isVisible = true;
    private String fieldType = "STANDARD";  // STANDARD, CUSTOM, ESCALATION
}