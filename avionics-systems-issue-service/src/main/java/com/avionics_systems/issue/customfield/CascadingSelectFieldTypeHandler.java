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
public class CascadingSelectFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private final CustomFieldOptionRepository optionRepository;

    @Override
    public String getType() { return "cascadingselect"; }

    @Override
    public String getDisplayName() { return "Select List (Cascading)"; }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        if (!(value instanceof Map)) {
            return ValidationResult.error("Cascading select requires {parent, child} object");
        }
        Map<String, Object> cascading = (Map<String, Object>) value;
        Object parentVal = cascading.get("parent");
        Object childVal = cascading.get("child");

        if (parentVal == null || parentVal.toString().isBlank()) {
            return isRequired(config) ? ValidationResult.error("Parent value is required") : ValidationResult.success();
        }

        UUID fieldId = getFieldIdFromConfig(config);
        if (fieldId == null) return ValidationResult.success(value);

        UUID parentId;
        try {
            parentId = UUID.fromString(parentVal.toString());
        } catch (IllegalArgumentException e) {
            return ValidationResult.error("Invalid parent option ID");
        }
        if (!optionRepository.existsByFieldIdAndId(fieldId, parentId)) {
            return ValidationResult.error("Invalid parent option: " + parentVal);
        }

        if (childVal != null && !childVal.toString().isBlank()) {
            UUID childId;
            try {
                childId = UUID.fromString(childVal.toString());
            } catch (IllegalArgumentException e) {
                return ValidationResult.error("Invalid child option ID");
            }
            List<CustomFieldOption> children = optionRepository.findByParentOptionIdOrderByPositionAsc(parentId);
            boolean validChild = children.stream().anyMatch(c -> c.getId().equals(childId));
            if (!validChild) {
                return ValidationResult.error("Child option does not belong to selected parent");
            }
        }

        return ValidationResult.success(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) return "";
        if (value instanceof Map) {
            Map<String, Object> cascading = (Map<String, Object>) value;
            StringBuilder sb = new StringBuilder();
            resolveOptionLabel(cascading.get("parent"), sb);
            resolveOptionLabel(cascading.get("child"), sb);
            return sb.toString().trim();
        }
        return value.toString();
    }

    private void resolveOptionLabel(Object optionVal, StringBuilder sb) {
        if (optionVal == null) return;
        try {
            UUID optionId = UUID.fromString(optionVal.toString());
            optionRepository.findById(optionId).ifPresent(o -> sb.append(o.getValue()).append(" "));
        } catch (IllegalArgumentException ignored) {
            sb.append(optionVal).append(" ");
        }
    }

    private UUID getFieldIdFromConfig(Map<String, Object> config) {
        if (config == null || !config.containsKey("fieldId")) return null;
        try { return UUID.fromString(config.get("fieldId").toString()); } catch (Exception e) { return null; }
    }
}
