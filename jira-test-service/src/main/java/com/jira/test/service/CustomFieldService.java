package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomFieldService {

    private final CustomFieldRepository customFieldRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");

    private static final int MAX_TEXT_LENGTH = 10000;
    private static final int MAX_FIELD_KEY_LENGTH = 255;
    private static final int MAX_OPTIONS = 100;

    @Transactional
    public CustomFieldResponse createCustomField(String name, String fieldKey, String fieldTypeStr, String options, String defaultValue, String validationRules, UUID projectId) {
        log.info("Creating custom field: {} with type: {}", name, fieldTypeStr);

        if (customFieldRepository.existsByFieldKey(fieldKey)) {
            throw new DuplicateResourceException("Field with key '" + fieldKey + "' already exists");
        }

        CustomField.FieldType fieldType;
        try {
            fieldType = CustomField.FieldType.valueOf(fieldTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid field type: " + fieldTypeStr +
                    ". Valid types are: TEXT, NUMBER, DATE, DATETIME, SELECT, MULTI_SELECT, CHECKBOX, RADIO, TEXTAREA, URL, EMAIL");
        }

        CustomField field = CustomField.builder()
                .name(name)
                .fieldKey(fieldKey)
                .fieldType(fieldType)
                .options(options)
                .defaultValue(defaultValue)
                .validationRules(validationRules)
                .projectId(projectId)
                .build();

        field = customFieldRepository.save(field);
        log.info("Custom field created with id: {}", field.getId());

        return mapToResponse(field);
    }

    @Transactional
    public CustomFieldResponse createCustomField(CreateCustomFieldRequest request) {
        return createCustomField(
                request.getName(),
                request.getFieldKey(),
                request.getFieldType(),
                request.getOptions(),
                request.getDefaultValue(),
                request.getValidationRules(),
                request.getProjectId()
        );
    }

    @Transactional(readOnly = true)
    public CustomFieldResponse getField(UUID fieldId) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));
        return mapToResponse(field);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldResponse> listFields() {
        return customFieldRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomFieldResponse> listFields(UUID projectId) {
        if (projectId != null) {
            return customFieldRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return listFields();
    }

    @Transactional
    public CustomFieldResponse updateField(UUID fieldId, UpdateCustomFieldRequest request) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        if (request.getName() != null) field.setName(request.getName());
        if (request.getDescription() != null) field.setDescription(request.getDescription());
        if (request.getOptions() != null) field.setOptions(request.getOptions());
        if (request.getDefaultValue() != null) field.setDefaultValue(request.getDefaultValue());
        if (request.getValidationRules() != null) field.setValidationRules(request.getValidationRules());

        field = customFieldRepository.save(field);
        log.info("Custom field updated: {}", fieldId);

        return mapToResponse(field);
    }

    @Transactional
    public void deleteField(UUID fieldId) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        customFieldRepository.delete(field);
        log.info("Custom field deleted: {}", fieldId);
    }

    @Transactional(readOnly = true)
    public List<CustomFieldResponse> searchFields(String searchTerm) {
        return customFieldRepository.findByNameContainingIgnoreCase(searchTerm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomFieldResponse> listFieldsByType(String fieldTypeStr) {
        CustomField.FieldType fieldType;
        try {
            fieldType = CustomField.FieldType.valueOf(fieldTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid field type: " + fieldTypeStr);
        }

        return customFieldRepository.findByFieldType(fieldType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomFieldResponse getFieldByKey(String fieldKey) {
        CustomField field = customFieldRepository.findByFieldKey(fieldKey)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "fieldKey", fieldKey));
        return mapToResponse(field);
    }

    @Transactional
    public CustomFieldOptionsResponse getFieldOptions(UUID fieldId) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        List<CustomFieldOption> options = new ArrayList<>();

        if (field.getOptions() != null && !field.getOptions().isEmpty()) {
            try {
                List<Map<String, Object>> parsedOptions = objectMapper.readValue(
                        field.getOptions(), new TypeReference<List<Map<String, Object>>>() {});

                for (Map<String, Object> opt : parsedOptions) {
                    options.add(CustomFieldOption.builder()
                            .value((String) opt.get("value"))
                            .label((String) opt.get("label"))
                            .position(opt.containsKey("position") ? ((Number) opt.get("position")).intValue() : 0)
                            .build());
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse options for field {}: {}", fieldId, e.getMessage());
            }
        }

        return CustomFieldOptionsResponse.builder()
                .fieldId(fieldId)
                .fieldType(field.getFieldType().name())
                .options(options)
                .build();
    }

    @Transactional
    public CustomFieldResponse updateFieldOptions(UUID fieldId, List<CustomFieldOptionUpdate> options) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        CustomField.FieldType fieldType = field.getFieldType();
        if (fieldType != CustomField.FieldType.SELECT &&
            fieldType != CustomField.FieldType.MULTI_SELECT &&
            fieldType != CustomField.FieldType.RADIO) {
            throw new ValidationException("Options can only be updated for SELECT, MULTI_SELECT, or RADIO field types");
        }

        if (options.size() > MAX_OPTIONS) {
            throw new ValidationException("Cannot have more than " + MAX_OPTIONS + " options");
        }

        Set<String> uniqueValues = new HashSet<>();
        for (CustomFieldOptionUpdate opt : options) {
            if (opt.getValue() == null || opt.getValue().isEmpty()) {
                throw new ValidationException("Option value cannot be empty");
            }
            if (!uniqueValues.add(opt.getValue())) {
                throw new ValidationException("Duplicate option value: " + opt.getValue());
            }
        }

        List<Map<String, Object>> optionsList = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            CustomFieldOptionUpdate opt = options.get(i);
            Map<String, Object> optMap = new LinkedHashMap<>();
            optMap.put("value", opt.getValue());
            optMap.put("label", opt.getLabel() != null ? opt.getLabel() : opt.getValue());
            optMap.put("position", opt.getPosition() != null ? opt.getPosition() : i);
            optionsList.add(optMap);
        }

        try {
            String optionsJson = objectMapper.writeValueAsString(optionsList);
            field.setOptions(optionsJson);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize options");
        }

        field = customFieldRepository.save(field);
        log.info("Updated {} options for field {}", options.size(), fieldId);

        return mapToResponse(field);
    }

    @Transactional
    public CustomFieldResponse addFieldOption(UUID fieldId, CustomFieldOptionUpdate option) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        List<CustomFieldOptionUpdate> currentOptions = new ArrayList<>();

        if (field.getOptions() != null && !field.getOptions().isEmpty()) {
            try {
                List<Map<String, Object>> parsed = objectMapper.readValue(
                        field.getOptions(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> opt : parsed) {
                    CustomFieldOptionUpdate optUpdate = new CustomFieldOptionUpdate();
                    optUpdate.setValue((String) opt.get("value"));
                    optUpdate.setLabel((String) opt.get("label"));
                    if (opt.containsKey("position")) {
                        optUpdate.setPosition(((Number) opt.get("position")).intValue());
                    }
                    currentOptions.add(optUpdate);
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse options: {}", e.getMessage());
            }
        }

        currentOptions.add(option);
        return updateFieldOptions(fieldId, currentOptions);
    }

    @Transactional
    public CustomFieldResponse removeFieldOption(UUID fieldId, String optionValue) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        List<CustomFieldOptionUpdate> currentOptions = new ArrayList<>();

        if (field.getOptions() != null && !field.getOptions().isEmpty()) {
            try {
                List<Map<String, Object>> parsed = objectMapper.readValue(
                        field.getOptions(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> opt : parsed) {
                    if (!opt.get("value").equals(optionValue)) {
                        CustomFieldOptionUpdate optUpdate = new CustomFieldOptionUpdate();
                        optUpdate.setValue((String) opt.get("value"));
                        optUpdate.setLabel((String) opt.get("label"));
                        if (opt.containsKey("position")) {
                            optUpdate.setPosition(((Number) opt.get("position")).intValue());
                        }
                        currentOptions.add(optUpdate);
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse options: {}", e.getMessage());
            }
        }

        return updateFieldOptions(fieldId, currentOptions);
    }

    @Transactional
    public CustomFieldResponse updateValidationRules(UUID fieldId, Map<String, Object> rules) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        try {
            String rulesJson = objectMapper.writeValueAsString(rules);
            field.setValidationRules(rulesJson);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize validation rules");
        }

        field = customFieldRepository.save(field);
        log.info("Updated validation rules for field {}", fieldId);

        return mapToResponse(field);
    }

    @Transactional
    public CustomFieldValidationReport validateFieldConfiguration(UUID fieldId) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        CustomField.FieldType fieldType = field.getFieldType();

        if (fieldType == CustomField.FieldType.SELECT ||
            fieldType == CustomField.FieldType.MULTI_SELECT ||
            fieldType == CustomField.FieldType.RADIO) {
            if (field.getOptions() == null || field.getOptions().isEmpty()) {
                errors.add("Select/Radio field has no options configured");
            }
        }

        if (fieldType == CustomField.FieldType.TEXT ||
            fieldType == CustomField.FieldType.TEXTAREA) {
            if (field.getValidationRules() != null) {
                try {
                    Map<String, Object> rules = objectMapper.readValue(
                            field.getValidationRules(), new TypeReference<Map<String, Object>>() {});
                    Integer maxLength = (Integer) rules.get("maxLength");
                    if (maxLength != null && maxLength > MAX_TEXT_LENGTH) {
                        warnings.add("Max length exceeds recommended maximum of " + MAX_TEXT_LENGTH);
                    }
                } catch (JsonProcessingException e) {
                    errors.add("Invalid validation rules format");
                }
            }
        }

        if (field.getFieldKey() != null && field.getFieldKey().length() > MAX_FIELD_KEY_LENGTH) {
            errors.add("Field key exceeds maximum length of " + MAX_FIELD_KEY_LENGTH);
        }

        return CustomFieldValidationReport.builder()
                .fieldId(fieldId)
                .fieldName(field.getName())
                .fieldType(fieldType.name())
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    @Transactional
    public List<CustomFieldResponse> cloneFieldsToProject(List<UUID> fieldIds, UUID targetProjectId) {
        List<CustomFieldResponse> clonedFields = new ArrayList<>();

        for (UUID sourceFieldId : fieldIds) {
            CustomField source = customFieldRepository.findById(sourceFieldId)
                    .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", sourceFieldId));

            String newFieldKey = source.getFieldKey();
            int counter = 1;
            while (customFieldRepository.existsByFieldKey(newFieldKey)) {
                newFieldKey = source.getFieldKey() + "_" + counter++;
            }

            CustomField cloned = CustomField.builder()
                    .name(source.getName())
                    .fieldKey(newFieldKey)
                    .fieldType(source.getFieldType())
                    .description(source.getDescription())
                    .options(source.getOptions())
                    .defaultValue(source.getDefaultValue())
                    .validationRules(source.getValidationRules())
                    .projectId(targetProjectId)
                    .build();

            cloned = customFieldRepository.save(cloned);
            clonedFields.add(mapToResponse(cloned));
        }

        log.info("Cloned {} custom fields to project {}", fieldIds.size(), targetProjectId);
        return clonedFields;
    }

    @Transactional
    public FieldValidationResult validateValue(UUID fieldId, String value) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomField", "id", fieldId));

        if (value == null || value.isEmpty()) {
            return FieldValidationResult.failure(fieldId, field.getName(), "Value cannot be empty");
        }

        boolean valid;
        String errorMessage = null;

        switch (field.getFieldType()) {
            case TEXT:
            case TEXTAREA:
                valid = validateText(value, field.getValidationRules());
                if (!valid) errorMessage = "Text value exceeds maximum length or contains invalid characters";
                break;

            case NUMBER:
                valid = validateNumber(value, field.getValidationRules());
                if (!valid) errorMessage = "Value must be a valid number and within specified range";
                break;

            case DATE:
                valid = validateDate(value);
                if (!valid) errorMessage = "Value must be a valid date (YYYY-MM-DD)";
                break;

            case DATETIME:
                valid = validateDateTime(value);
                if (!valid) errorMessage = "Value must be a valid datetime (YYYY-MM-DDTHH:mm:ss)";
                break;

            case EMAIL:
                valid = EMAIL_PATTERN.matcher(value).matches();
                if (!valid) errorMessage = "Value must be a valid email address";
                break;

            case URL:
                valid = URL_PATTERN.matcher(value).matches();
                if (!valid) errorMessage = "Value must be a valid URL";
                break;

            case SELECT:
            case MULTI_SELECT:
                valid = validateSelect(value, field.getOptions());
                if (!valid) errorMessage = "Value must be one of the allowed options";
                break;

            case CHECKBOX:
                valid = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
                if (!valid) errorMessage = "Value must be true or false";
                break;

            case RADIO:
                valid = validateSelect(value, field.getOptions());
                if (!valid) errorMessage = "Value must be one of the allowed options";
                break;

            default:
                valid = true;
        }

        if (valid) {
            return FieldValidationResult.success(fieldId, field.getName());
        } else {
            return FieldValidationResult.failure(fieldId, field.getName(), errorMessage);
        }
    }

    @Transactional
    public List<FieldValidationResult> validateValues(Map<UUID, String> fieldValues) {
        List<FieldValidationResult> results = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : fieldValues.entrySet()) {
            results.add(validateValue(entry.getKey(), entry.getValue()));
        }
        return results;
    }

    private boolean validateText(String value, String validationRules) {
        if (value == null || value.length() > 10000) {
            return false;
        }

        if (validationRules != null) {
            try {
                Map<String, Object> rules = objectMapper.readValue(validationRules, new TypeReference<Map<String, Object>>() {});
                Integer maxLength = (Integer) rules.get("maxLength");
                if (maxLength != null && value.length() > maxLength) {
                    return false;
                }
                Integer minLength = (Integer) rules.get("minLength");
                if (minLength != null && value.length() < minLength) {
                    return false;
                }
                String pattern = (String) rules.get("pattern");
                if (pattern != null && !Pattern.matches(pattern, value)) {
                    return false;
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse validation rules: {}", e.getMessage());
            }
        }

        return true;
    }

    private boolean validateNumber(String value, String validationRules) {
        try {
            double num = Double.parseDouble(value);

            if (validationRules != null) {
                try {
                    Map<String, Object> rules = objectMapper.readValue(validationRules, new TypeReference<Map<String, Object>>() {});
                    Double min = (Double) rules.get("min");
                    if (min != null && num < min) {
                        return false;
                    }
                    Double max = (Double) rules.get("max");
                    if (max != null && num > max) {
                        return false;
                    }
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse validation rules: {}", e.getMessage());
                }
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateDate(String value) {
        return value.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private boolean validateDateTime(String value) {
        return value.matches("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}(:\\d{2})?");
    }

    private boolean validateSelect(String value, String optionsJson) {
        if (optionsJson == null || optionsJson.isEmpty()) {
            return true;
        }

        try {
            List<String> options = objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
            return options.contains(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse select options: {}", e.getMessage());
            return true;
        }
    }

    private CustomFieldResponse mapToResponse(CustomField field) {
        Object options = null;
        Object defaultValue = null;
        Object validationRules = null;

        if (field.getOptions() != null) {
            try {
                options = objectMapper.readValue(field.getOptions(), Object.class);
            } catch (JsonProcessingException e) {
                options = field.getOptions();
            }
        }

        if (field.getDefaultValue() != null) {
            try {
                defaultValue = objectMapper.readValue(field.getDefaultValue(), Object.class);
            } catch (JsonProcessingException e) {
                defaultValue = field.getDefaultValue();
            }
        }

        if (field.getValidationRules() != null) {
            try {
                validationRules = objectMapper.readValue(field.getValidationRules(), Object.class);
            } catch (JsonProcessingException e) {
                validationRules = field.getValidationRules();
            }
        }

        return CustomFieldResponse.builder()
                .id(field.getId())
                .projectId(field.getProjectId())
                .name(field.getName())
                .fieldKey(field.getFieldKey())
                .fieldType(field.getFieldType().name())
                .description(field.getDescription())
                .options(options)
                .defaultValue(defaultValue)
                .validationRules(validationRules)
                .createdAt(field.getCreatedAt())
                .updatedAt(field.getUpdatedAt())
                .build();
    }
}