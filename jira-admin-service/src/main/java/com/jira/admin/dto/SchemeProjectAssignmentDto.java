package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemeProjectAssignmentDto {
    private String id;
    private String projectKey;
    private String name;
    private String status;
    private boolean assigned;
    private String currentSchemeId;
    private String currentSchemeName;
}
