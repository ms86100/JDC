package com.avionics_systems.project.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class ValidateCreateIssueFieldsRequest {
    private UUID issueTypeId;
    private Map<String, Object> fields;
}
