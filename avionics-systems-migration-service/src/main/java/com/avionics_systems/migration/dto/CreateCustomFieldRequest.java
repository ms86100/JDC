package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomFieldRequest {
    private String name;
    private String description;
    private String type;
    private String searcherKey;
    private String rendererKey;
    private Boolean navigable;
    private Map<String, Object> config;
    private Map<String, Object> defaultValues;
    private List<Map<String, Object>> options;
    private List<UUID> projectIds;
    private List<UUID> issueTypeIds;
}