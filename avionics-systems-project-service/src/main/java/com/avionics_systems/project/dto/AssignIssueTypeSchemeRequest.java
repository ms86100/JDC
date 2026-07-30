package com.avionics_systems.project.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignIssueTypeSchemeRequest {
    private String schemeName;
    private String description;
    private List<String> issueTypeKeys;
    private String defaultIssueTypeKey;
    private List<String> projectIds;
}
