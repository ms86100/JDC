package com.avionics_systems.issue.customfield;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectPickerFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public String getType() { return "projectpicker"; }

    @Override
    public String getDisplayName() { return "Project Picker"; }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null || value.toString().isBlank()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        try {
            UUID projectId = UUID.fromString(value.toString());
            if (projectExists(projectId)) {
                return ValidationResult.success(value);
            }
            return ValidationResult.error("Project not found: " + value);
        } catch (IllegalArgumentException e) {
            return ValidationResult.error("Invalid project ID format");
        }
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) return "";
        try {
            UUID projectId = UUID.fromString(value.toString());
            return resolveProjectName(projectId);
        } catch (Exception e) {
            return value.toString();
        }
    }

    private boolean projectExists(UUID projectId) {
        try {
            restTemplate.getForObject(projectServiceUrl + "/api/projects/" + projectId, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveProjectName(UUID projectId) {
        try {
            Map<String, Object> project = restTemplate.getForObject(
                    projectServiceUrl + "/api/projects/" + projectId, Map.class);
            if (project != null && project.get("name") != null) return project.get("name").toString();
        } catch (Exception ignored) {}
        return projectId.toString();
    }
}
