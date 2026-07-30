package com.avionics_systems.migration.dto;

import com.avionics_systems.migration.entity.field.CustomFieldDefinition;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDefinitionResponse {
    private UUID id;
    private String name;
    private String description;
    private String type;
    private String searcherKey;
    private String rendererKey;
    private String fieldKey;
    private Boolean enabled;
    private Boolean searchable;
    private Boolean navigable;
    private List<CustomFieldOptionResponse> options;
    private List<CustomFieldContextResponse> contexts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomFieldOptionResponse {
        private UUID id;
        private String value;
        private String label;
        private String description;
        private String color;
        private Integer sequence;
        private Boolean disabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomFieldContextResponse {
        private UUID id;
        private String name;
        private Boolean allProjects;
        private List<UUID> projectIds;
        private List<UUID> issueTypeIds;
    }

    public static CustomFieldDefinitionResponse fromEntity(CustomFieldDefinition entity) {
        return CustomFieldDefinitionResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .searcherKey(entity.getSearcherKey())
                .rendererKey(entity.getRendererKey())
                .fieldKey(entity.getFieldKey())
                .enabled(entity.getEnabled())
                .searchable(entity.getSearchable())
                .navigable(entity.getNavigable())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}