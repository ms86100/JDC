package com.jira.test.service;

import com.jira.test.entity.CustomField;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FieldTypeRegistry {

    private final Map<CustomField.FieldType, FieldTypeConfig> typeRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeDefaults() {
        // TEXT type
        FieldTypeConfig textConfig = new FieldTypeConfig();
        textConfig.setType(CustomField.FieldType.TEXT);
        textConfig.setDisplayName("Text Field");
        textConfig.setDescription("Single line text input");
        textConfig.setDefaultMaxLength(255);
        textConfig.setSupportsMinLength(true);
        textConfig.setSupportsMaxLength(true);
        textConfig.setSupportsRegex(true);
        textConfig.setSupportsDefaultValue(true);
        textConfig.setEditorComponent("text-input");
        textConfig.setDisplayComponent("text-display");
        textConfig.setValidationFunction(value -> validateText(value, 255));
        textConfig.setFormatFunction(this::formatTextValue);
        registerType(CustomField.FieldType.TEXT, textConfig);

        // TEXTAREA type
        FieldTypeConfig textareaConfig = new FieldTypeConfig();
        textareaConfig.setType(CustomField.FieldType.TEXTAREA);
        textareaConfig.setDisplayName("Text Area");
        textareaConfig.setDescription("Multi-line text input");
        textareaConfig.setDefaultMaxLength(10000);
        textareaConfig.setSupportsMinLength(true);
        textareaConfig.setSupportsMaxLength(true);
        textareaConfig.setSupportsRegex(true);
        textareaConfig.setSupportsDefaultValue(true);
        textareaConfig.setEditorComponent("textarea-input");
        textareaConfig.setDisplayComponent("text-display");
        textareaConfig.setValidationFunction(value -> validateText(value, 10000));
        textareaConfig.setFormatFunction(this::formatTextValue);
        registerType(CustomField.FieldType.TEXTAREA, textareaConfig);

        // NUMBER type
        FieldTypeConfig numberConfig = new FieldTypeConfig();
        numberConfig.setType(CustomField.FieldType.NUMBER);
        numberConfig.setDisplayName("Number");
        numberConfig.setDescription("Numeric input with optional decimal places");
        numberConfig.setSupportsMinValue(true);
        numberConfig.setSupportsMaxValue(true);
        numberConfig.setSupportsDefaultValue(true);
        numberConfig.setEditorComponent("number-input");
        numberConfig.setDisplayComponent("number-display");
        numberConfig.setValidationFunction(this::validateNumber);
        numberConfig.setFormatFunction(this::formatNumberValue);
        registerType(CustomField.FieldType.NUMBER, numberConfig);

        // DATE type
        FieldTypeConfig dateConfig = new FieldTypeConfig();
        dateConfig.setType(CustomField.FieldType.DATE);
        dateConfig.setDisplayName("Date");
        dateConfig.setDescription("Date picker");
        dateConfig.setDefaultFormat("yyyy-MM-dd");
        dateConfig.setSupportsDefaultValue(true);
        dateConfig.setEditorComponent("date-picker");
        dateConfig.setDisplayComponent("date-display");
        dateConfig.setValidationFunction(this::validateDate);
        dateConfig.setFormatFunction(this::formatDateValue);
        registerType(CustomField.FieldType.DATE, dateConfig);

        // DATETIME type
        FieldTypeConfig datetimeConfig = new FieldTypeConfig();
        datetimeConfig.setType(CustomField.FieldType.DATETIME);
        datetimeConfig.setDisplayName("Date Time");
        datetimeConfig.setDescription("Date and time picker");
        datetimeConfig.setDefaultFormat("yyyy-MM-dd'T'HH:mm:ss");
        datetimeConfig.setSupportsDefaultValue(true);
        datetimeConfig.setEditorComponent("datetime-picker");
        datetimeConfig.setDisplayComponent("datetime-display");
        datetimeConfig.setValidationFunction(this::validateDateTime);
        datetimeConfig.setFormatFunction(this::formatDateTimeValue);
        registerType(CustomField.FieldType.DATETIME, datetimeConfig);

        // SELECT type
        FieldTypeConfig selectConfig = new FieldTypeConfig();
        selectConfig.setType(CustomField.FieldType.SELECT);
        selectConfig.setDisplayName("Select");
        selectConfig.setDescription("Single selection dropdown");
        selectConfig.setSupportsOptions(true);
        selectConfig.setSupportsDefaultValue(true);
        selectConfig.setEditorComponent("select-dropdown");
        selectConfig.setDisplayComponent("select-display");
        selectConfig.setValidationFunction(this::validateSelect);
        selectConfig.setFormatFunction(this::formatSelectValue);
        registerType(CustomField.FieldType.SELECT, selectConfig);

        // MULTI_SELECT type
        FieldTypeConfig multiSelectConfig = new FieldTypeConfig();
        multiSelectConfig.setType(CustomField.FieldType.MULTI_SELECT);
        multiSelectConfig.setDisplayName("Multi-Select");
        multiSelectConfig.setDescription("Multiple selection dropdown");
        multiSelectConfig.setSupportsOptions(true);
        multiSelectConfig.setSupportsDefaultValue(true);
        multiSelectConfig.setEditorComponent("multiselect-dropdown");
        multiSelectConfig.setDisplayComponent("multiselect-display");
        multiSelectConfig.setValidationFunction(this::validateMultiSelect);
        multiSelectConfig.setFormatFunction(this::formatMultiSelectValue);
        registerType(CustomField.FieldType.MULTI_SELECT, multiSelectConfig);

        // CHECKBOX type
        FieldTypeConfig checkboxConfig = new FieldTypeConfig();
        checkboxConfig.setType(CustomField.FieldType.CHECKBOX);
        checkboxConfig.setDisplayName("Checkbox");
        checkboxConfig.setDescription("Boolean checkbox");
        checkboxConfig.setEditorComponent("checkbox-input");
        checkboxConfig.setDisplayComponent("checkbox-display");
        checkboxConfig.setValidationFunction(this::validateCheckbox);
        checkboxConfig.setFormatFunction(this::formatCheckboxValue);
        registerType(CustomField.FieldType.CHECKBOX, checkboxConfig);

        // RADIO type
        FieldTypeConfig radioConfig = new FieldTypeConfig();
        radioConfig.setType(CustomField.FieldType.RADIO);
        radioConfig.setDisplayName("Radio Button");
        radioConfig.setDescription("Single selection radio buttons");
        radioConfig.setSupportsOptions(true);
        radioConfig.setSupportsDefaultValue(true);
        radioConfig.setEditorComponent("radio-buttons");
        radioConfig.setDisplayComponent("radio-display");
        radioConfig.setValidationFunction(this::validateRadio);
        radioConfig.setFormatFunction(this::formatSelectValue);
        registerType(CustomField.FieldType.RADIO, radioConfig);

        // LABEL type
        FieldTypeConfig labelConfig = new FieldTypeConfig();
        labelConfig.setType(CustomField.FieldType.LABEL);
        labelConfig.setDisplayName("Label");
        labelConfig.setDescription("Read-only text label");
        labelConfig.setDefaultMaxLength(100);
        labelConfig.setSupportsMinLength(true);
        labelConfig.setSupportsMaxLength(true);
        labelConfig.setEditorComponent("label-input");
        labelConfig.setDisplayComponent("label-display");
        labelConfig.setValidationFunction(value -> validateText(value, 100));
        labelConfig.setFormatFunction(this::formatTextValue);
        registerType(CustomField.FieldType.LABEL, labelConfig);

        // URL type
        FieldTypeConfig urlConfig = new FieldTypeConfig();
        urlConfig.setType(CustomField.FieldType.URL);
        urlConfig.setDisplayName("URL");
        urlConfig.setDescription("Website URL");
        urlConfig.setDefaultMaxLength(2048);
        urlConfig.setSupportsRegex(false);
        urlConfig.setSupportsDefaultValue(true);
        urlConfig.setEditorComponent("url-input");
        urlConfig.setDisplayComponent("url-display");
        urlConfig.setValidationFunction(this::validateUrl);
        urlConfig.setFormatFunction(this::formatUrlValue);
        registerType(CustomField.FieldType.URL, urlConfig);

        // EMAIL type
        FieldTypeConfig emailConfig = new FieldTypeConfig();
        emailConfig.setType(CustomField.FieldType.EMAIL);
        emailConfig.setDisplayName("Email");
        emailConfig.setDescription("Email address");
        emailConfig.setDefaultMaxLength(255);
        emailConfig.setSupportsRegex(false);
        emailConfig.setSupportsDefaultValue(true);
        emailConfig.setEditorComponent("email-input");
        emailConfig.setDisplayComponent("email-display");
        emailConfig.setValidationFunction(this::validateEmail);
        emailConfig.setFormatFunction(this::formatEmailValue);
        registerType(CustomField.FieldType.EMAIL, emailConfig);

        // USER_PICKER type
        FieldTypeConfig userPickerConfig = new FieldTypeConfig();
        userPickerConfig.setType(CustomField.FieldType.USER_PICKER);
        userPickerConfig.setDisplayName("User Picker (Single)");
        userPickerConfig.setDescription("Single user selection");
        userPickerConfig.setSupportsDefaultValue(true);
        userPickerConfig.setEditorComponent("user-picker");
        userPickerConfig.setDisplayComponent("user-display");
        userPickerConfig.setValidationFunction(this::validateUuidReference);
        userPickerConfig.setFormatFunction(this::formatUserReference);
        registerType(CustomField.FieldType.USER_PICKER, userPickerConfig);

        // USER_PICKER_MULTI type
        FieldTypeConfig userPickerMultiConfig = new FieldTypeConfig();
        userPickerMultiConfig.setType(CustomField.FieldType.USER_PICKER_MULTI);
        userPickerMultiConfig.setDisplayName("User Picker (Multiple)");
        userPickerMultiConfig.setDescription("Multiple user selection");
        userPickerMultiConfig.setSupportsDefaultValue(true);
        userPickerMultiConfig.setEditorComponent("user-picker-multi");
        userPickerMultiConfig.setDisplayComponent("user-multi-display");
        userPickerMultiConfig.setValidationFunction(this::validateUuidReference);
        userPickerMultiConfig.setFormatFunction(this::formatMultiValue);
        registerType(CustomField.FieldType.USER_PICKER_MULTI, userPickerMultiConfig);

        // PROJECT_PICKER type
        FieldTypeConfig projectPickerConfig = new FieldTypeConfig();
        projectPickerConfig.setType(CustomField.FieldType.PROJECT_PICKER);
        projectPickerConfig.setDisplayName("Project Picker");
        projectPickerConfig.setDescription("Single project selection");
        projectPickerConfig.setSupportsDefaultValue(true);
        projectPickerConfig.setEditorComponent("project-picker");
        projectPickerConfig.setDisplayComponent("project-display");
        projectPickerConfig.setValidationFunction(this::validateUuidReference);
        projectPickerConfig.setFormatFunction(this::formatTextValue);
        registerType(CustomField.FieldType.PROJECT_PICKER, projectPickerConfig);

        // VERSION_PICKER type
        FieldTypeConfig versionPickerConfig = new FieldTypeConfig();
        versionPickerConfig.setType(CustomField.FieldType.VERSION_PICKER);
        versionPickerConfig.setDisplayName("Version Picker (Single)");
        versionPickerConfig.setDescription("Single version selection");
        versionPickerConfig.setSupportsDefaultValue(true);
        versionPickerConfig.setEditorComponent("version-picker");
        versionPickerConfig.setDisplayComponent("version-display");
        versionPickerConfig.setValidationFunction(this::validateUuidReference);
        versionPickerConfig.setFormatFunction(this::formatTextValue);
        registerType(CustomField.FieldType.VERSION_PICKER, versionPickerConfig);

        // VERSION_PICKER_MULTI type
        FieldTypeConfig versionPickerMultiConfig = new FieldTypeConfig();
        versionPickerMultiConfig.setType(CustomField.FieldType.VERSION_PICKER_MULTI);
        versionPickerMultiConfig.setDisplayName("Version Picker (Multiple)");
        versionPickerMultiConfig.setDescription("Multiple version selection");
        versionPickerMultiConfig.setSupportsDefaultValue(true);
        versionPickerMultiConfig.setEditorComponent("version-picker-multi");
        versionPickerMultiConfig.setDisplayComponent("version-multi-display");
        versionPickerMultiConfig.setValidationFunction(this::validateUuidReference);
        versionPickerMultiConfig.setFormatFunction(this::formatMultiValue);
        registerType(CustomField.FieldType.VERSION_PICKER_MULTI, versionPickerMultiConfig);

        // LABELS type
        FieldTypeConfig labelsConfig = new FieldTypeConfig();
        labelsConfig.setType(CustomField.FieldType.LABELS);
        labelsConfig.setDisplayName("Labels");
        labelsConfig.setDescription("Tag-style labels");
        labelsConfig.setDefaultMaxLength(255);
        labelsConfig.setSupportsMinLength(false);
        labelsConfig.setSupportsMaxLength(true);
        labelsConfig.setSupportsDefaultValue(true);
        labelsConfig.setEditorComponent("labels-input");
        labelsConfig.setDisplayComponent("labels-display");
        labelsConfig.setValidationFunction(this::validateLabels);
        labelsConfig.setFormatFunction(this::formatLabelsValue);
        registerType(CustomField.FieldType.LABELS, labelsConfig);

        // CASCADING_SELECT type
        FieldTypeConfig cascadingConfig = new FieldTypeConfig();
        cascadingConfig.setType(CustomField.FieldType.CASCADING_SELECT);
        cascadingConfig.setDisplayName("Cascading Select");
        cascadingConfig.setDescription("Parent-child hierarchical options");
        cascadingConfig.setSupportsOptions(true);
        cascadingConfig.setSupportsDefaultValue(true);
        cascadingConfig.setEditorComponent("cascading-select");
        cascadingConfig.setDisplayComponent("cascading-display");
        cascadingConfig.setValidationFunction(this::validateCascadingSelect);
        cascadingConfig.setFormatFunction(this::formatCascadingValue);
        registerType(CustomField.FieldType.CASCADING_SELECT, cascadingConfig);

        log.info("FieldTypeRegistry initialized with {} field types", typeRegistry.size());
    }

    public void registerType(CustomField.FieldType type, FieldTypeConfig config) {
        typeRegistry.put(type, config);
        log.debug("Registered field type: {}", type);
    }

    public Optional<FieldTypeConfig> getTypeConfig(CustomField.FieldType type) {
        return Optional.ofNullable(typeRegistry.get(type));
    }

    public FieldTypeConfig getRequiredTypeConfig(CustomField.FieldType type) {
        FieldTypeConfig config = typeRegistry.get(type);
        if (config == null) {
            throw new IllegalArgumentException("Unknown field type: " + type);
        }
        return config;
    }

    public boolean isValidType(CustomField.FieldType type) {
        return typeRegistry.containsKey(type);
    }

    public List<FieldTypeConfig> getAllTypes() {
        return new ArrayList<>(typeRegistry.values());
    }

    public List<FieldTypeConfig> getTypesByCategory(String category) {
        return typeRegistry.values().stream()
                .filter(config -> category.equals(config.getCategory()))
                .collect(Collectors.toList());
    }

    public FieldValidationResult validateValue(CustomField.FieldType type, String value, Map<String, Object> validationRules) {
        FieldTypeConfig config = getRequiredTypeConfig(type);
        return config.getValidationFunction().apply(value);
    }

    public String formatValue(CustomField.FieldType type, String value) {
        FieldTypeConfig config = getRequiredTypeConfig(type);
        return config.getFormatFunction().apply(value);
    }

    public String getEditorComponent(CustomField.FieldType type) {
        return getRequiredTypeConfig(type).getEditorComponent();
    }

    public String getDisplayComponent(CustomField.FieldType type) {
        return getRequiredTypeConfig(type).getDisplayComponent();
    }

    // Validation functions
    private FieldValidationResult validateText(String value, int maxLength) {
        if (value == null) {
            return FieldValidationResult.success("Value is valid");
        }
        if (value.length() > maxLength) {
            return FieldValidationResult.error("Value exceeds maximum length of " + maxLength + " characters");
        }
        return FieldValidationResult.success("Value is valid");
    }

    private FieldValidationResult validateNumber(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        try {
            Double.parseDouble(value);
            return FieldValidationResult.success("Value is valid");
        } catch (NumberFormatException e) {
            return FieldValidationResult.error("Value must be a valid number");
        }
    }

    private FieldValidationResult validateDate(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return FieldValidationResult.success("Value is valid");
        }
        return FieldValidationResult.error("Value must be a valid date in format YYYY-MM-DD");
    }

    private FieldValidationResult validateDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        if (value.matches("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}(:\\d{2})?")) {
            return FieldValidationResult.success("Value is valid");
        }
        return FieldValidationResult.error("Value must be a valid datetime in format YYYY-MM-DDTHH:mm:ss");
    }

    private FieldValidationResult validateSelect(String value) {
        return FieldValidationResult.success("Select field validation delegated to CustomFieldService");
    }

    private FieldValidationResult validateMultiSelect(String value) {
        return FieldValidationResult.success("Multi-select field validation delegated to CustomFieldService");
    }

    private FieldValidationResult validateCheckbox(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return FieldValidationResult.success("Value is valid");
        }
        return FieldValidationResult.error("Value must be true or false");
    }

    private FieldValidationResult validateRadio(String value) {
        return FieldValidationResult.success("Radio field validation delegated to CustomFieldService");
    }

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");

    private FieldValidationResult validateUrl(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        if (URL_PATTERN.matcher(value).matches()) {
            return FieldValidationResult.success("Value is valid");
        }
        return FieldValidationResult.error("Value must be a valid URL");
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private FieldValidationResult validateEmail(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return FieldValidationResult.success("Value is valid");
        }
        return FieldValidationResult.error("Value must be a valid email address");
    }

    private FieldValidationResult validateUuidReference(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        try {
            java.util.UUID.fromString(value);
            return FieldValidationResult.success("Value is valid");
        } catch (IllegalArgumentException e) {
            return FieldValidationResult.error("Value must be a valid UUID");
        }
    }

    private FieldValidationResult validateLabels(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        if (value.length() > 255) {
            return FieldValidationResult.error("Label exceeds maximum length of 255 characters");
        }
        return FieldValidationResult.success("Value is valid");
    }

    private FieldValidationResult validateCascadingSelect(String value) {
        if (value == null || value.isEmpty()) {
            return FieldValidationResult.success("Value is valid");
        }
        if (value.contains(";")) {
            String[] parts = value.split(";");
            if (parts.length == 2) {
                try {
                    java.util.UUID.fromString(parts[0].trim());
                    java.util.UUID.fromString(parts[1].trim());
                    return FieldValidationResult.success("Value is valid");
                } catch (IllegalArgumentException e) {
                    return FieldValidationResult.error("Cascading select values must be valid UUIDs separated by semicolon");
                }
            }
        }
        return FieldValidationResult.error("Cascading select must be in format: parentUUID;childUUID");
    }

    // Format functions
    private String formatTextValue(String value) {
        return value != null ? value : "";
    }

    private String formatNumberValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            double num = Double.parseDouble(value);
            if (num == Math.floor(num)) {
                return String.valueOf((long) num);
            }
            return String.valueOf(num);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String formatDateValue(String value) {
        return value != null ? value : "";
    }

    private String formatDateTimeValue(String value) {
        return value != null ? value : "";
    }

    private String formatSelectValue(String value) {
        return value != null ? value : "";
    }

    private String formatMultiSelectValue(String value) {
        return value != null ? value : "";
    }

    private String formatCheckboxValue(String value) {
        if (value == null || value.isEmpty()) {
            return "No";
        }
        return value.equalsIgnoreCase("true") ? "Yes" : "No";
    }

    private String formatUrlValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return "https://" + value;
        }
        return value;
    }

    private String formatEmailValue(String value) {
        return value != null ? value.toLowerCase() : "";
    }

    private String formatUserReference(String value) {
        return value != null ? value : "";
    }

    private String formatMultiValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replace(",", ", ");
    }

    private String formatLabelsValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value;
    }

    private String formatCascadingValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldTypeConfig {
        private CustomField.FieldType type;
        private String displayName;
        private String description;
        private String category;

        // Text-specific
        private Integer defaultMaxLength;
        private boolean supportsMinLength;
        private boolean supportsMaxLength;
        private boolean supportsRegex;

        // Number-specific
        private boolean supportsMinValue;
        private boolean supportsMaxValue;

        // Select-specific
        private boolean supportsOptions;

        // Date-specific
        private String defaultFormat;

        // Common
        private boolean supportsDefaultValue;

        private String editorComponent;
        private String displayComponent;

        private Function<String, FieldValidationResult> validationFunction;
        private Function<String, String> formatFunction;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldValidationResult {
        private boolean valid;
        private String message;

        public static FieldValidationResult success(String message) {
            return new FieldValidationResult(true, message);
        }

        public static FieldValidationResult error(String message) {
            return new FieldValidationResult(false, message);
        }
    }
}