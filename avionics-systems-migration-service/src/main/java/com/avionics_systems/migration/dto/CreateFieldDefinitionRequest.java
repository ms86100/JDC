package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFieldDefinitionRequest {
    private String fieldKey;
    private String displayName;
    private String description;
    private String fieldType;
    private String renderer;
    private String screenRegion;
    private Boolean searchable;
    private Boolean sortable;
    private Boolean filterable;
    private Boolean required;
    private Map<String, Object> schemaDefinition;
    private Map<String, Object> visibilityRules;
    private Map<String, Object> rendererConfig;
    private Map<String, Object> validationRules;
    private List<Map<String, Object>> options;
}