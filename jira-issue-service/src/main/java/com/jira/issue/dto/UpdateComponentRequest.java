package com.jira.issue.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComponentRequest {

    private String name;
    private String description;
    private UUID leadId;
    private String assigneeType;
    private UUID defaultAssigneeId;
    private Boolean isAssigneeTypeEnabled;
}