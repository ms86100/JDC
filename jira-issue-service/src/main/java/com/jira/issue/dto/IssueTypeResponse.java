package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private String issueTypeKey;
    private boolean isSubtask;
    private String icon;
    private String color;
    private int sequence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}