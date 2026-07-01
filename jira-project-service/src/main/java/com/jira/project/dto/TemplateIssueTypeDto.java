package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for issue type in template response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateIssueTypeDto {
    private UUID id;
    private String issueTypeName;
    private String issueTypeIcon;
    private Boolean isDefault;
    private Boolean isSubtask;
    private Integer sequence;
}