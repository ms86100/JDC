package com.avionics_systems.migration.persister;

import com.avionics_systems.migration.entity.field.CustomFieldDefinition;
import com.avionics_systems.migration.entity.field.CustomFieldOption;
import com.avionics_systems.migration.entity.field.FieldDefinition;
import com.avionics_systems.migration.exception.*;
import com.avionics_systems.migration.repository.field.CustomFieldDefinitionRepository;
import com.avionics_systems.migration.repository.field.CustomFieldOptionRepository;
import com.avionics_systems.migration.repository.field.FieldDefinitionRepository;
import com.avionics_systems.migration.service.field.FieldProvisioningService;
import com.avionics_systems.migration.service.field.FieldScreenConfigurationService;
import com.avionics_systems.migration.service.field.FieldValueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Custom Field Persister Handler — persists definitions via {@link FieldProvisioningService}
 * and values via {@link FieldValueService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomFieldPersisterHandler {

    private static final UUID SYSTEM_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final Map<String, String> FIELD_TYPE_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("TEXTFIELD", "text");
        map.put("TEXTAREA", "textarea");
        map.put("DATEPICKER", "datepicker");
        map.put("DATETIME", "datetime");
        map.put("NUMBER", "number");
        map.put("FLOAT", "number");
        map.put("CHECKBOX", "checkbox");
        map.put("RADIOBUTTON", "select");
        map.put("SELECT", "select");
        map.put("MULTISELECT", "multiselect");
        map.put("USERPICKER", "userpicker");
        map.put("URL", "url");
        FIELD_TYPE_MAP = Collections.unmodifiableMap(map);
    }

    private final FieldProvisioningService fieldProvisioningService;
    private final FieldValueService fieldValueService;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CustomFieldOptionRepository customFieldOptionRepository;
    private final FieldScreenConfigurationService fieldScreenConfigurationService;

    @Transactional(rollbackFor = Exception.class)
    public CustomFieldPersistResult persistCustomField(Map<String, Object> fieldData, UUID jobId) {
        CustomFieldPersistResult result = new CustomFieldPersistResult();

        try {
            String fieldName = (String) fieldData.get("name");
            if (fieldName == null || fieldName.isBlank()) {
                throw new ValidationException("Custom field name is required", "FIELD_NAME_REQUIRED", "name");
            }

            String fieldType = (String) fieldData.getOrDefault("fieldType", fieldData.get("type"));
            if (fieldType == null) {
                fieldType = "TEXTFIELD";
            }

            UUID projectId = fieldData.get("projectId") instanceof UUID u
                    ? u
                    : (fieldData.get("projectId") != null
                    ? UUID.fromString(fieldData.get("projectId").toString())
                    : null);

            String mappedType = FIELD_TYPE_MAP.getOrDefault(fieldType.toUpperCase(Locale.ROOT), fieldType.toLowerCase(Locale.ROOT));
            FieldDefinition saved = fieldProvisioningService.provisionCustomField(fieldName, mappedType, SYSTEM_USER);
            fieldScreenConfigurationService.ensureFieldVisibleOnScreen(saved.getFieldKey(), projectId);

            UUID customFieldId = customFieldDefinitionRepository.findByFieldKey(saved.getFieldKey())
                    .map(CustomFieldDefinition::getId)
                    .orElse(saved.getId());

            if (isSelectField(fieldType)) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> options = (List<Map<String, Object>>) fieldData.get("options");
                persistFieldOptions(customFieldId, options);
            }

            result.setSuccess(true);
            result.setFieldId(customFieldId);
            result.setFieldName(fieldName);

            log.info("Persisted custom field: {} ({}) id={}", fieldName, fieldType, customFieldId);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw new MigrationException("Failed to persist custom field: " + e.getMessage(), e);
        }

        return result;
    }

    private void persistFieldOptions(UUID customFieldId, List<Map<String, Object>> options) {
        if (options == null || options.isEmpty()) {
            return;
        }
        int seq = 0;
        for (Map<String, Object> option : options) {
            String optionValue = option.get("value") != null ? option.get("value").toString() : null;
            if (optionValue == null || optionValue.isBlank()) {
                continue;
            }
            String label = option.get("label") != null ? option.get("label").toString() : optionValue;
            CustomFieldOption entity = CustomFieldOption.builder()
                    .customFieldId(customFieldId)
                    .value(optionValue)
                    .label(label)
                    .sequence(option.get("sequence") instanceof Number n ? n.intValue() : seq++)
                    .disabled(Boolean.TRUE.equals(option.get("disabled")))
                    .build();
            customFieldOptionRepository.save(entity);
        }
    }

    private boolean isSelectField(String fieldType) {
        return Set.of("SELECT", "RADIOBUTTON", "MULTISELECT", "CASCADING_SELECT")
                .contains(fieldType.toUpperCase(Locale.ROOT));
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistCustomFieldValues(UUID issueId, Map<String, Object> customFieldValues, UUID jobId) {
        if (customFieldValues == null || customFieldValues.isEmpty() || issueId == null) {
            return;
        }

        UUID actor = jobId != null ? jobId : SYSTEM_USER;
        Map<String, Object> resolved = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : customFieldValues.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null || (value instanceof String s && s.isBlank())) {
                continue;
            }
            String fieldKey = resolveFieldKey(key);
            if (fieldKey == null) {
                log.warn("Skipping custom field value — unknown key: {}", key);
                continue;
            }
            resolved.put(fieldKey, value);
        }

        if (!resolved.isEmpty()) {
            fieldValueService.setFieldValues(issueId, resolved, actor);
            log.debug("Persisted {} custom field values for issue {}", resolved.size(), issueId);
        }
    }

    private String resolveFieldKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");
        if (fieldDefinitionRepository.existsByFieldKey(normalized)) {
            return normalized;
        }
        if (fieldDefinitionRepository.existsByFieldKey(key)) {
            return key;
        }
        if (key.startsWith("customfield_")) {
            return fieldDefinitionRepository.findByFieldKey(key).map(FieldDefinition::getFieldKey).orElse(key);
        }
        return fieldDefinitionRepository.findByFieldKey("customfield_" + normalized)
                .map(FieldDefinition::getFieldKey)
                .orElse(normalized);
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
}
