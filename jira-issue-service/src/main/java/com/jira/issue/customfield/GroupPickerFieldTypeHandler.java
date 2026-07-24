package com.jira.issue.customfield;

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
public class GroupPickerFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    @Value("${user.service.url:http://jira-user-service:8082}")
    private String userServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public String getType() { return "grouppicker"; }

    @Override
    public String getDisplayName() { return "Group Picker"; }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null || value.toString().isBlank()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        try {
            UUID groupId = UUID.fromString(value.toString());
            if (!groupExists(groupId)) {
                return ValidationResult.error("Group not found: " + value);
            }
            return ValidationResult.success(value);
        } catch (IllegalArgumentException e) {
            return ValidationResult.error("Invalid group ID format");
        }
    }

    private boolean groupExists(UUID groupId) {
        try {
            restTemplate.getForObject(userServiceUrl + "/rest/admin/1.0/groups/" + groupId, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) return "";
        try {
            UUID groupId = UUID.fromString(value.toString());
            return resolveGroupName(groupId);
        } catch (Exception e) {
            return value.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveGroupName(UUID groupId) {
        try {
            Map<String, Object> group = restTemplate.getForObject(
                    userServiceUrl + "/rest/admin/1.0/groups/" + groupId, Map.class);
            if (group != null && group.get("groupName") != null) return group.get("groupName").toString();
        } catch (Exception ignored) {}
        return groupId.toString();
    }
}
