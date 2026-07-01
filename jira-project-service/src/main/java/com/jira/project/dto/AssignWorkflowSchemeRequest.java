package com.jira.project.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignWorkflowSchemeRequest {
    /** Canonical scheme id from jira-workflow-service */
    private UUID schemeId;
    private String schemeName;
    private String description;
    private List<String> projectIds;
}
