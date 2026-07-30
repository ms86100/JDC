package com.avionics_systems.issue.customfield;

import com.avionics_systems.issue.entity.ProjectVersion;
import com.avionics_systems.issue.repository.ProjectVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VersionPickerFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private final ProjectVersionRepository versionRepository;

    @Override
    public String getType() { return "versionpicker"; }

    @Override
    public String getDisplayName() { return "Version Picker"; }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null || value.toString().isBlank()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        try {
            UUID versionId = UUID.fromString(value.toString());
            if (versionRepository.existsById(versionId)) {
                return ValidationResult.success(value);
            }
            return ValidationResult.error("Version not found: " + value);
        } catch (IllegalArgumentException e) {
            return ValidationResult.error("Invalid version ID format");
        }
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) return "";
        try {
            UUID versionId = UUID.fromString(value.toString());
            return versionRepository.findById(versionId)
                    .map(ProjectVersion::getName).orElse(value.toString());
        } catch (Exception e) {
            return value.toString();
        }
    }
}
