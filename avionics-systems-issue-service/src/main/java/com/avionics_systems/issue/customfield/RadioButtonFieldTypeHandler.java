package com.avionics_systems.issue.customfield;

import com.avionics_systems.issue.entity.CustomFieldOption;
import com.avionics_systems.issue.repository.CustomFieldOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RadioButtonFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private final CustomFieldOptionRepository optionRepository;

    @Override
    public String getType() { return "radiobutton"; }

    @Override
    public String getDisplayName() { return "Radio Buttons"; }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null || value.toString().isBlank()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        UUID fieldId = getFieldIdFromConfig(config);
        if (fieldId == null) return ValidationResult.success(value);

        List<CustomFieldOption> options = optionRepository.findByFieldIdAndDisabledFalseOrderByPositionAsc(fieldId);
        String val = value.toString();
        boolean valid = options.stream().anyMatch(o -> o.getId().toString().equals(val) || o.getValue().equals(val));
        return valid ? ValidationResult.success(value) : ValidationResult.error("Invalid option: " + val);
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) return "";
        return value.toString().toLowerCase();
    }

    private UUID getFieldIdFromConfig(Map<String, Object> config) {
        if (config == null || !config.containsKey("fieldId")) return null;
        try { return UUID.fromString(config.get("fieldId").toString()); } catch (Exception e) { return null; }
    }
}
