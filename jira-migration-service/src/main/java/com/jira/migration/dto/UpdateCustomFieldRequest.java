package com.jira.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomFieldRequest {
    private String name;
    private String description;
    private String type;
    private String searcherKey;
    private String rendererKey;
    private Boolean enabled;
    private Boolean searchable;
    private Boolean navigable;
    private Map<String, Object> config;
    private List<Map<String, Object>> options;
}
