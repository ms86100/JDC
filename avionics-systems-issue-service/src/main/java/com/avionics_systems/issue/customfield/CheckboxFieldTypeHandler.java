package com.avionics_systems.issue.customfield;

import com.avionics_systems.issue.entity.CustomFieldOption;
import com.avionics_systems.issue.repository.CustomFieldOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CheckboxFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private final CustomFieldOptionRepository optionRepository;

    @Override
    public String getType() { return "checkbox"; }

    @Override
    public String getDisplayName() { return "Checkboxes"; }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        List<String> values;
        if (value instanceof List) {
            values = ((List<Object>) value).stream().map(Object::toString).collect(Collectors.toList());
        } else {
            values = List.of(value.toString());
        }
        if (values.isEmpty()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        UUID fieldId = getFieldIdFromConfig(config);
        if (fieldId == null) return ValidationResult.success(value);

        List<CustomFieldOption> options = optionRepository.findByFieldIdAndDisabledFalseOrderByPositionAsc(fieldId);
        Set<String> validIds = options.stream().map(o -> o.getId().toString()).collect(Collectors.toSet());
        Set<String> validValues = options.stream().map(CustomFieldOption::getValue).collect(Collectors.toSet());

        for (String v : values) {
            if (!validIds.contains(v) && !validValues.contains(v)) {
                return ValidationResult.error("Invalid option: " + v);
            }
        }
        return ValidationResult.success(values);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) return "";
        if (value instanceof List) {
            return ((List<Object>) value).stream().map(Object::toString).collect(Collectors.joining(" "));
        }
        return value.toString();
    }

    private UUID getFieldIdFromConfig(Map<String, Object> config) {
        if (config == null || !config.containsKey("fieldId")) return null;
        try { return UUID.fromString(config.get("fieldId").toString()); } catch (Exception e) { return null; }
    }
}
