package com.jira.migration.dto;

import com.jira.migration.entity.field.FieldDefinition;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinitionResponse {
    private UUID id;
    private String fieldKey;
    private String displayName;
    private String description;
    private String fieldType;
    private String renderer;
    private String screenRegion;
    private String pluginSource;
    private Boolean searchable;
    private Boolean sortable;
    private Boolean filterable;
    private Boolean required;
    private Boolean readOnly;
    private Boolean hidden;
    private Boolean custom;
    private Boolean builtIn;
    private Boolean deprecated;
    private Map<String, Object> schemaDefinition;
    private Map<String, Object> visibilityRules;
    private Map<String, Object> rendererConfig;
    private Map<String, Object> validationRules;
    private List<FieldOptionResponse> options;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOptionResponse {
        private String value;
        private String label;
        private Integer order;
        private String color;
        private Boolean disabled;
    }

    public static FieldDefinitionResponse fromEntity(FieldDefinition entity) {
        List<FieldOptionResponse> options = null;
        if (entity.getOptions() != null) {
            options = entity.getOptions().stream()
                    .map(opt -> FieldOptionResponse.builder()
                            .value(opt.getValue())
                            .label(opt.getLabel())
                            .order(opt.getOrder())
                            .color(opt.getColor())
                            .disabled(opt.getDisabled())
                            .build())
                    .toList();
        }

        return FieldDefinitionResponse.builder()
                .id(entity.getId())
                .fieldKey(entity.getFieldKey())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .fieldType(entity.getFieldType() != null ? entity.getFieldType().name() : null)
                .renderer(entity.getRenderer() != null ? entity.getRenderer().name() : null)
                .screenRegion(entity.getScreenRegion() != null ? entity.getScreenRegion().name() : null)
                .pluginSource(entity.getPluginSource())
                .searchable(entity.getSearchable())
                .sortable(entity.getSortable())
                .filterable(entity.getFilterable())
                .required(entity.getRequired())
                .readOnly(entity.getReadOnly())
                .hidden(entity.getHidden())
                .custom(entity.getCustom())
                .builtIn(entity.getBuiltIn())
                .deprecated(entity.getDeprecated())
                .schemaDefinition(entity.getSchemaDefinition())
                .visibilityRules(entity.getVisibilityRules())
                .rendererConfig(entity.getRendererConfig())
                .validationRules(entity.getValidationRules())
                .options(options)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}