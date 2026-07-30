package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFieldDefinitionRequest {
    private String displayName;
    private String description;
    private String renderer;
    private String screenRegion;
    private Boolean searchable;
    private Boolean sortable;
    private Boolean filterable;
    private Boolean required;
    private Boolean readOnly;
    private Boolean hidden;
    private Boolean deprecated;
    private Map<String, Object> schemaDefinition;
    private Map<String, Object> visibilityRules;
    private Map<String, Object> rendererConfig;
    private Map<String, Object> validationRules;
}