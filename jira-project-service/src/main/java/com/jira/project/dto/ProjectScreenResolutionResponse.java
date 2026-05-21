package com.jira.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScreenResolutionResponse {
    private UUID projectId;
    private UUID issueTypeId;
    private UUID createScreenId;
    private UUID editScreenId;
    private UUID viewScreenId;
    private UUID defaultScreenId;
}
