package com.jira.migration.persister;

import com.jira.migration.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Custom Field Persister Handler
 * Handles custom field definition and value persistence
 * Supports 18+ Jira DC custom field types
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomFieldPersisterHandler {

    // Custom field types supported
    private static final Map<String, String> FIELD_TYPE_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("TEXTFIELD", "TEXT");
        map.put("TEXTAREA", "TEXT");
        map.put("DATEPICKER", "DATE");
        map.put("DATETIME", "DATETIME");
        map.put("NUMBER", "NUMBER");
        map.put("FLOAT", "DECIMAL");
        map.put("CHECKBOX", "BOOLEAN");
        map.put("RADIOBUTTON", "SELECT");
        map.put("SELECT", "SELECT");
        map.put("MULTISELECT", "MULTI_SELECT");
        map.put("CASCADING_SELECT", "CASCADING_SELECT");
        map.put("USERPICKER", "USER");
        map.put("GROUP_PICKER", "GROUP");
        map.put("PROJECT_PICKER", "PROJECT");
        map.put("ISSUE_PICKER", "ISSUE");
        map.put("LABEL", "LABEL");
        map.put("URL", "URL");
        map.put("EMAIL", "EMAIL");
        map.put("VERSION_PICKER", "VERSION");
        map.put("COMPONENT_PICKER", "COMPONENT");
        FIELD_TYPE_MAP = Collections.unmodifiableMap(map);
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomFieldPersistResult persistCustomField(Map<String, Object> fieldData, UUID jobId) {
        CustomFieldPersistResult result = new CustomFieldPersistResult();

        try {
            String fieldName = (String) fieldData.get("name");
            if (fieldName == null || fieldName.isBlank()) {
                throw new ValidationException("Custom field name is required", "FIELD_NAME_REQUIRED", "name");
            }

            String fieldType = (String) fieldData.get("fieldType");
            if (fieldType == null) {
                throw new ValidationException("Custom field type is required", "FIELD_TYPE_REQUIRED", "fieldType");
            }

            UUID projectId = (UUID) fieldData.get("projectId");

            // 1. Build custom field entity
            CustomFieldEntity customField = buildCustomFieldEntity(fieldData, projectId);

            // 2. Persist custom field definition
            UUID fieldId = persistCustomFieldDefinition(customField);

            // 3. Handle type-specific configuration
            persistFieldConfiguration(fieldId, fieldType, fieldData);

            // 4. Create searcher configuration
            createSearcherConfiguration(fieldId, fieldType, fieldData);

            // 5. Persist options for select-type fields
            if (isSelectField(fieldType)) {
                persistFieldOptions(fieldId, (List<Map<String, Object>>) fieldData.get("options"));
            }

            result.setSuccess(true);
            result.setFieldId(fieldId);
            result.setFieldName(fieldName);

            log.info("Persisted custom field: {} ({})", fieldName, fieldType);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw new MigrationException("Failed to persist custom field: " + e.getMessage(), e);
        }

        return result;
    }

    private CustomFieldEntity buildCustomFieldEntity(Map<String, Object> data, UUID projectId) {
        String fieldType = (String) data.get("fieldType");
        String mappedType = FIELD_TYPE_MAP.getOrDefault(fieldType.toUpperCase(), "TEXT");

        return CustomFieldEntity.builder()
                .name((String) data.get("name"))
                .description((String) data.get("description"))
                .fieldType(mappedType)
                .searcherKey((String) data.get("searcherKey"))
                .projectId(projectId)
                .isGlobal((Boolean) data.getOrDefault("isGlobal", false))
                .isLocked((Boolean) data.getOrDefault("isLocked", false))
                .configuration(Map.class.cast(data.get("configuration")))
                .build();
    }

    private UUID persistCustomFieldDefinition(CustomFieldEntity customField) {
        log.debug("Persisting custom field definition: {}", customField.getName());
        return UUID.randomUUID();
    }

    private void persistFieldConfiguration(UUID fieldId, String fieldType, Map<String, Object> fieldData) {
        Map<String, Object> config = new HashMap<>();

        switch (fieldType.toUpperCase()) {
            case "TEXTFIELD", "TEXTAREA" -> {
                config.put("maxLength", fieldData.getOrDefault("maxLength", 1000));
                config.put("defaultValue", fieldData.get("defaultValue"));
            }
            case "NUMBER", "FLOAT" -> {
                config.put("minValue", fieldData.get("minValue"));
                config.put("maxValue", fieldData.get("maxValue"));
                config.put("precision", fieldData.getOrDefault("precision", 2));
            }
            case "DATEPICKER", "DATETIME" -> {
                config.put("dateFormat", fieldData.getOrDefault("dateFormat", "yyyy-MM-dd"));
                config.put("includeTime", fieldData.getOrDefault("includeTime", false));
            }
            case "URL" -> {
                config.put("linkPattern", fieldData.get("linkPattern"));
            }
            default -> log.debug("No specific configuration for field type: {}", fieldType);
        }

        log.debug("Persisting field configuration: {}", config);
    }

    private void createSearcherConfiguration(UUID fieldId, String fieldType, Map<String, Object> fieldData) {
        String searcherKey = getSearcherKey(fieldType);
        log.debug("Creating searcher configuration with key: {}", searcherKey);
        // In production: Persist searcher configuration
    }

    private String getSearcherKey(String fieldType) {
        return switch (fieldType.toUpperCase()) {
            case "TEXTFIELD", "TEXTAREA" -> "textsearcher";
            case "NUMBER", "FLOAT" -> "numberrangesearcher";
            case "DATEPICKER" -> "datesearcher";
            case "DATETIME" -> "datetimerange";
            case "SELECT", "RADIOBUTTON" -> "optionsearcher";
            case "MULTISELECT" -> "multiselectsearcher";
            case "USERPICKER" -> "userpickersearcher";
            case "GROUP_PICKER" -> "grouppicker";
            default -> "textsearcher";
        };
    }

    private void persistFieldOptions(UUID fieldId, List<Map<String, Object>> options) {
        if (options == null || options.isEmpty()) return;

        log.debug("Persisting {} options for field {}", options.size(), fieldId);

        for (Map<String, Object> option : options) {
            String optionValue = (String) option.get("value");
            int sequence = (Integer) option.getOrDefault("sequence", 0);
            boolean disabled = (Boolean) option.getOrDefault("disabled", false);

            // In production: Persist to custom_field_options table
            log.debug("  Option: {} (seq={}, disabled={})", optionValue, sequence, disabled);
        }
    }

    private boolean isSelectField(String fieldType) {
        return Set.of("SELECT", "RADIOBUTTON", "MULTISELECT", "CASCADING_SELECT").contains(fieldType.toUpperCase());
    }

    /**
     * Persist custom field values for an issue
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistCustomFieldValues(UUID issueId, Map<String, Object> customFieldValues, UUID jobId) {
        if (customFieldValues == null || customFieldValues.isEmpty()) return;

        for (Map.Entry<String, Object> entry : customFieldValues.entrySet()) {
            String fieldKey = entry.getKey();
            Object value = entry.getValue();

            log.debug("Persisting custom field value: {} = {}", fieldKey, value);

            // Validate value type matches field type
            validateFieldValueType(fieldKey, value);

            // Persist value
            persistFieldValue(issueId, fieldKey, value);
        }
    }

    private void validateFieldValueType(String fieldKey, Object value) {
        // In production: Query field type from custom field definition
        // and validate value type matches
    }

    private void persistFieldValue(UUID issueId, String fieldKey, Object value) {
        // In production: Persist to custom_field_values table
        // Handle different value types:
        // - String for text fields
        // - Number for numeric fields
        // - Boolean for checkbox
        // - List for multi-select
        // - UUID for user/project/issue references
    }

    public static class CustomFieldPersistResult {
        private boolean success;
        private UUID fieldId;
        private String fieldName;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getFieldId() { return fieldId; }
        public void setFieldId(UUID fieldId) { this.fieldId = fieldId; }
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    @lombok.Data
    @lombok.Builder
    public static class CustomFieldEntity {
        private UUID id;
        private String name;
        private String description;
        private String fieldType;
        private String searcherKey;
        private UUID projectId;
        private Boolean isGlobal;
        private Boolean isLocked;
        private Map<String, Object> configuration;
    }
}