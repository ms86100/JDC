package com.jira.migration.service.field;

import com.jira.migration.entity.field.FieldDefinition;
import com.jira.migration.entity.field.IssueFieldValue;
import com.jira.migration.repository.field.FieldDefinitionRepository;
import com.jira.migration.repository.field.IssueFieldValueRepository;
import com.jira.migration.exception.EntityNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing dynamic field values.
 * Handles storage, retrieval, and formatting of issue field values.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldValueService {

    private final IssueFieldValueRepository fieldValueRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final ObjectMapper objectMapper;

    public record FieldValueResult(
            Map<String, Object> values,
            Map<String, FieldDefinition> definitions,
            List<String> validationErrors
    ) {}

    @Transactional
    public void setFieldValue(UUID issueId, String fieldKey, Object value, UUID userId) {
        FieldDefinition fieldDef = fieldDefinitionRepository.findByFieldKey(fieldKey)
                .orElseThrow(() -> new EntityNotFoundException("FieldDefinition", fieldKey));

        setFieldValue(issueId, fieldDef, value, userId);
    }

    @Transactional
    public void setFieldValue(UUID issueId, FieldDefinition fieldDef, Object value, UUID userId) {
        Optional<IssueFieldValue> existing = fieldValueRepository.findByIssueIdAndFieldDefinitionId(
                issueId, fieldDef.getId());

        IssueFieldValue fieldValue;
        if (existing.isPresent()) {
            fieldValue = existing.get();
            fieldValue.setVersion(fieldValue.getVersion() + 1);
            fieldValue.setUpdatedBy(userId);
        } else {
            fieldValue = IssueFieldValue.builder()
                    .issueId(issueId)
                    .fieldDefinitionId(fieldDef.getId())
                    .createdBy(userId)
                    .build();
        }

        setValue(fieldValue, fieldDef, value);
        fieldValueRepository.save(fieldValue);

        log.debug("Set field value: issue={}, field={}", issueId, fieldDef.getFieldKey());
    }

    @Transactional
    public void setFieldValues(UUID issueId, Map<String, Object> values, UUID userId) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            try {
                setFieldValue(issueId, entry.getKey(), entry.getValue(), userId);
            } catch (Exception e) {
                log.error("Failed to set field value: {} for issue {}", entry.getKey(), issueId, e);
            }
        }
    }

    @Transactional(readOnly = true)
    public Object getFieldValue(UUID issueId, String fieldKey) {
        FieldDefinition fieldDef = fieldDefinitionRepository.findByFieldKey(fieldKey)
                .orElse(null);

        if (fieldDef == null) return null;

        Optional<IssueFieldValue> fieldValue = fieldValueRepository.findByIssueIdAndFieldDefinitionId(
                issueId, fieldDef.getId());

        return fieldValue.map(fv -> getValue(fv, fieldDef)).orElse(null);
    }

    @Transactional(readOnly = true)
    public FieldValueResult getAllFieldValues(UUID issueId) {
        List<IssueFieldValue> fieldValues = fieldValueRepository.findByIssueIdWithFieldDefinition(issueId);
        Map<String, Object> values = new HashMap<>();
        Map<String, FieldDefinition> definitions = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();

        for (IssueFieldValue fv : fieldValues) {
            FieldDefinition fieldDef = fv.getFieldDefinition();
            if (fieldDef != null) {
                Object value = getValue(fv, fieldDef);
                values.put(fieldDef.getFieldKey(), value);
                definitions.put(fieldDef.getFieldKey(), fieldDef);

                if (!"VALID".equals(fv.getValidationStatus())) {
                    validationErrors.add(fieldDef.getFieldKey() + ": " + fv.getValidationMessage());
                }
            }
        }

        return new FieldValueResult(values, definitions, validationErrors);
    }

    @Transactional
    public void deleteFieldValue(UUID issueId, String fieldKey) {
        FieldDefinition fieldDef = fieldDefinitionRepository.findByFieldKey(fieldKey)
                .orElse(null);

        if (fieldDef != null) {
            fieldValueRepository.deleteByIssueIdAndFieldDefinitionId(issueId, fieldDef.getId());
        }
    }

    @Transactional
    public void deleteAllFieldValues(UUID issueId) {
        fieldValueRepository.deleteByIssueId(issueId);
    }

    /**
     * Delete all field values for a specific field definition.
     * Used when a field definition is deleted/deprecated to prevent orphaned values.
     */
    @Transactional
    public void deleteFieldValuesByFieldDefinitionId(UUID fieldDefinitionId) {
        // First count for logging
        List<IssueFieldValue> fieldValues = fieldValueRepository.findByFieldDefinitionId(fieldDefinitionId);
        int count = fieldValues.size();

        // Use optimized bulk delete
        fieldValueRepository.deleteByFieldDefinitionId(fieldDefinitionId);

        log.info("Deleted {} field values for field definition: {}", count, fieldDefinitionId);
    }

    @Transactional
    public IssueFieldValue validateFieldValue(UUID issueId, String fieldKey, Object value) {
        FieldDefinition fieldDef = fieldDefinitionRepository.findByFieldKey(fieldKey)
                .orElse(null);

        if (fieldDef == null) {
            return null;
        }

        Optional<IssueFieldValue> existing = fieldValueRepository.findByIssueIdAndFieldDefinitionId(
                issueId, fieldDef.getId());

        IssueFieldValue fieldValue;
        if (existing.isPresent()) {
            fieldValue = existing.get();
        } else {
            fieldValue = IssueFieldValue.builder()
                    .issueId(issueId)
                    .fieldDefinitionId(fieldDef.getId())
                    .validationStatus(IssueFieldValue.ValidationStatus.PENDING.name())
                    .build();
        }

        validateAndSetValue(fieldValue, fieldDef, value);
        return fieldValueRepository.save(fieldValue);
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> getFieldOptions(String fieldKey) {
        FieldDefinition fieldDef = fieldDefinitionRepository.findByFieldKey(fieldKey)
                .orElse(null);

        if (fieldDef == null || fieldDef.getOptions() == null) {
            return Map.of();
        }

        List<String> options = fieldDef.getOptions().stream()
                .filter(opt -> !Boolean.TRUE.equals(opt.getDisabled()))
                .map(FieldDefinition.FieldOption::getLabel)
                .toList();

        return Map.of("options", options);
    }

    private void setValue(IssueFieldValue fieldValue, FieldDefinition fieldDef, Object value) {
        fieldValue.setRawValue(value != null ? value.toString() : null);
        fieldValue.setSearchableText(generateSearchableText(value));

        switch (fieldDef.getFieldType()) {
            case TEXT, TEXTAREA, RICHTEXT -> {
                fieldValue.setStringValue(value != null ? value.toString() : null);
                fieldValue.setFormattedValue(formatTextValue(value, fieldDef));
            }
            case NUMBER, STORY_POINTS, DURATION -> {
                if (value instanceof Number num) {
                    fieldValue.setLongValue(num.longValue());
                    fieldValue.setFormattedValue(formatNumberValue(num, fieldDef));
                } else if (value instanceof String str) {
                    try {
                        long parsed = Long.parseLong(str);
                        fieldValue.setLongValue(parsed);
                        fieldValue.setFormattedValue(formatNumberValue(parsed, fieldDef));
                    } catch (NumberFormatException e) {
                        fieldValue.setStringValue(str);
                    }
                }
            }
            case DATE -> {
                if (value instanceof LocalDate date) {
                    fieldValue.setDateValue(date);
                } else if (value instanceof String str) {
                    try {
                        fieldValue.setDateValue(LocalDate.parse(str));
                    } catch (Exception e) {
                        fieldValue.setStringValue(str);
                    }
                }
            }
            case DATETIME -> {
                if (value instanceof LocalDateTime dt) {
                    fieldValue.setDatetimeValue(dt);
                } else if (value instanceof String str) {
                    try {
                        fieldValue.setDatetimeValue(LocalDateTime.parse(str));
                    } catch (Exception e) {
                        fieldValue.setStringValue(str);
                    }
                }
            }
            case CHECKBOX -> {
                fieldValue.setBooleanValue(Boolean.TRUE.equals(value) ||
                        "true".equalsIgnoreCase(String.valueOf(value)));
            }
            case SINGLE_SELECT, ISSUE_TYPE, STATUS, PRIORITY, RESOLUTION,
                    COMPONENT, VERSION, SECURITY_LEVEL, EPIC, SPRINT -> {
                fieldValue.setStringValue(value != null ? value.toString() : null);
                fieldValue.setFormattedValue(formatSelectValue(value, fieldDef));
            }
            case MULTI_SELECT, LABEL, USER, GROUP -> {
                try {
                    List<?> list = parseAsList(value);
                    @SuppressWarnings("unchecked")
                    List<Object> objList = new java.util.ArrayList<>(list);
                    fieldValue.setArrayValue(objList.stream().map(Object::toString).toList());
                    fieldValue.setFormattedValue(formatMultiSelectValue(list, fieldDef));
                } catch (Exception e) {
                    fieldValue.setStringValue(value != null ? value.toString() : null);
                }
            }
            case PROJECT -> {
                fieldValue.setStringValue(value != null ? value.toString() : null);
            }
            case URL, EMAIL -> {
                fieldValue.setStringValue(value != null ? value.toString() : null);
                fieldValue.setFormattedValue(value != null ? value.toString() : null);
            }
            case CUSTOM, UNKNOWN -> {
                try {
                    Map<String, Object> obj = objectMapper.convertValue(value,
                            new TypeReference<>() {});
                    fieldValue.setObjectValue(obj);
                    fieldValue.setFormattedValue(objectMapper.writeValueAsString(value));
                } catch (Exception e) {
                    fieldValue.setStringValue(value != null ? value.toString() : null);
                }
            }
            default -> {
                fieldValue.setStringValue(value != null ? value.toString() : null);
            }
        }

        fieldValue.setValidationStatus(IssueFieldValue.ValidationStatus.VALID.name());
        fieldValue.setValidationMessage(null);
    }

    private Object getValue(IssueFieldValue fieldValue, FieldDefinition fieldDef) {
        switch (fieldDef.getFieldType()) {
            case TEXT, TEXTAREA, RICHTEXT, SINGLE_SELECT, ISSUE_TYPE, STATUS, PRIORITY,
                    RESOLUTION, COMPONENT, VERSION, SECURITY_LEVEL, EPIC, SPRINT, PROJECT,
                    URL, EMAIL -> {
                return fieldValue.getStringValue();
            }
            case NUMBER, STORY_POINTS, DURATION -> {
                return fieldValue.getLongValue();
            }
            case DATE -> {
                return fieldValue.getDateValue();
            }
            case DATETIME -> {
                return fieldValue.getDatetimeValue();
            }
            case CHECKBOX -> {
                return fieldValue.getBooleanValue();
            }
            case MULTI_SELECT, LABEL, USER, GROUP -> {
                return fieldValue.getArrayValue();
            }
            case CUSTOM, UNKNOWN -> {
                return fieldValue.getObjectValue() != null ?
                        fieldValue.getObjectValue() : fieldValue.getStringValue();
            }
            default -> {
                return fieldValue.getStringValue();
            }
        }
    }

    private void validateAndSetValue(IssueFieldValue fieldValue, FieldDefinition fieldDef, Object value) {
        fieldValue.setRawValue(value != null ? value.toString() : null);
        fieldValue.setValidationStatus(IssueFieldValue.ValidationStatus.VALID.name());
        fieldValue.setValidationMessage(null);

        if (fieldDef.getRequired() && (value == null ||
                (value instanceof String && ((String) value).isEmpty()))) {
            fieldValue.setValidationStatus(IssueFieldValue.ValidationStatus.INVALID.name());
            fieldValue.setValidationMessage("Required field cannot be empty");
            return;
        }

        Map<String, Object> validationRules = fieldDef.getValidationRules();
        if (validationRules != null) {
            String pattern = (String) validationRules.get("pattern");
            if (pattern != null && value instanceof String str) {
                if (!str.matches(pattern)) {
                    fieldValue.setValidationStatus(IssueFieldValue.ValidationStatus.INVALID.name());
                    fieldValue.setValidationMessage("Value does not match required pattern");
                }
            }

            Integer max = (Integer) validationRules.get("max");
            if (max != null && value instanceof Number num) {
                if (num.longValue() > max) {
                    fieldValue.setValidationStatus(IssueFieldValue.ValidationStatus.WARNING.name());
                    fieldValue.setValidationMessage("Value exceeds maximum of " + max);
                }
            }
        }

        setValue(fieldValue, fieldDef, value);
    }

    private String generateSearchableText(Object value) {
        if (value == null) return "";
        if (value instanceof String) return (String) value;
        if (value instanceof List) {
            return String.join(" ", ((List<?>) value).stream()
                    .map(Object::toString)
                    .toList());
        }
        return value.toString();
    }

    private String formatTextValue(Object value, FieldDefinition fieldDef) {
        if (value == null) return "";
        String str = value.toString();
        int maxLength = 200;
        Map<String, Object> schema = fieldDef.getSchemaDefinition();
        if (schema != null && schema.containsKey("maxLength")) {
            maxLength = (Integer) schema.get("maxLength");
        }
        if (str.length() > maxLength) {
            return str.substring(0, maxLength) + "...";
        }
        return str;
    }

    private String formatNumberValue(Number value, FieldDefinition fieldDef) {
        if (value == null) return "";
        if (fieldDef.getFieldType() == FieldDefinition.FieldType.DURATION) {
            return formatDuration(value.longValue());
        }
        return value.toString();
    }

    private String formatSelectValue(Object value, FieldDefinition fieldDef) {
        if (value == null) return "";
        String str = value.toString();

        if (fieldDef.getOptions() != null) {
            return fieldDef.getOptions().stream()
                    .filter(opt -> opt.getValue().equals(str) || opt.getLabel().equals(str))
                    .findFirst()
                    .map(FieldDefinition.FieldOption::getLabel)
                    .orElse(str);
        }

        return str;
    }

    private String formatMultiSelectValue(List<?> values, FieldDefinition fieldDef) {
        if (values == null || values.isEmpty()) return "";

        List<String> labels = new ArrayList<>();
        for (Object val : values) {
            String label = formatSelectValue(val, fieldDef);
            labels.add(label);
        }

        return String.join(", ", labels);
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours >= 24) {
            long days = hours / 24;
            long remainingHours = hours % 24;
            return days + "d " + remainingHours + "h";
        }
        if (minutes > 0) {
            return hours + "h " + minutes + "m";
        }
        return hours + "h";
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseAsList(Object value) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        if (value instanceof String str) {
            return List.of(str.split(","));
        }
        return List.of(value);
    }

    @Transactional(readOnly = true)
    public List<UUID> findIssuesWithFieldValue(String fieldKey, Object value) {
        FieldDefinition fieldDef = fieldDefinitionRepository.findByFieldKey(fieldKey)
                .orElse(null);

        if (fieldDef == null) {
            return List.of();
        }

        List<IssueFieldValue> fieldValues = fieldValueRepository.findByIssueIdOrderByFieldDefinitionId(
                fieldDef.getId());

        return fieldValues.stream()
                .filter(fv -> value.equals(getValue(fv, fieldDef)))
                .map(IssueFieldValue::getIssueId)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getFieldValueStatistics(UUID fieldDefId) {
        List<IssueFieldValue> fieldValues = fieldValueRepository.findByIssueIdOrderByFieldDefinitionId(
                fieldDefId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalValues", (long) fieldValues.size());
        stats.put("nullValues", fieldValues.stream().filter(fv -> fv.getStringValue() == null).count());
        stats.put("uniqueValues", fieldValues.stream()
                .map(IssueFieldValue::getStringValue)
                .filter(Objects::nonNull)
                .distinct()
                .count());

        return stats;
    }
}