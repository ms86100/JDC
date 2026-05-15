package com.jira.issue.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private UUID leadId;
    private String leadName;
    private String assigneeType;
    private UUID defaultAssigneeId;
    private String defaultAssigneeName;
    private Boolean isAssigneeTypeEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Issue counts
    private Long issueCount;
    private Long assigneeIssueCount;
}