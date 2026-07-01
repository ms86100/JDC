package com.jira.portal.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequestTypeRequest {

    private String name;
    private String description;
    private String issueType;
    private UUID issueTypeId;
    private UUID projectId;
    private String fieldsConfig;
    private String instructions;
    private Boolean isEnabled = true;
    private Boolean isDefault = false;
    private String iconUrl;
    private Integer displayOrder = 0;
    private Integer slaMinutes = 480;
    private UUID workflowId;
}