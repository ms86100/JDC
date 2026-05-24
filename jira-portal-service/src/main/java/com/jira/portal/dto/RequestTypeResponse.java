package com.jira.portal.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestTypeResponse {

    private UUID id;
    private UUID portalId;
    private String name;
    private String description;
    private String issueType;
    private UUID issueTypeId;
    private UUID projectId;
    private String fieldsConfig;
    private String instructions;
    private Boolean isEnabled;
    private Boolean isDefault;
    private String iconUrl;
    private Integer displayOrder;
    private Integer slaMinutes;
}