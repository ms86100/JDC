package com.avionics_systems.project.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignWorkflowSchemeRequest {
    /** Canonical scheme id from avionics-systems-workflow-service */
    private UUID schemeId;
    private String schemeName;
    private String description;
    private List<String> projectIds;
}
