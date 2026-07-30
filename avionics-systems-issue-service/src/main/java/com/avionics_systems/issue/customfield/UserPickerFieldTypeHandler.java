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
public class UserPickerFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    @Value("${user.service.url:http://avionics-systems-user-service:8082}")
    private String userServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public String getType() { return "userpicker"; }

    @Override
    public String getDisplayName() { return "User Picker (Single User)"; }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null || value.toString().isBlank()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        try {
            UUID userId = UUID.fromString(value.toString());
            if (userExists(userId)) {
                return ValidationResult.success(value);
            }
            return ValidationResult.error("User not found: " + value);
        } catch (IllegalArgumentException e) {
            return ValidationResult.error("Invalid user ID format");
        }
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) return "";
        try {
            UUID userId = UUID.fromString(value.toString());
            return resolveUserName(userId);
        } catch (Exception e) {
            return value.toString();
        }
    }

    private boolean userExists(UUID userId) {
        try {
            restTemplate.getForObject(userServiceUrl + "/api/users/" + userId, Map.class);
            return true;
        } catch (Exception e) {
            log.debug("User lookup failed for {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveUserName(UUID userId) {
        try {
            Map<String, Object> user = restTemplate.getForObject(userServiceUrl + "/api/users/" + userId, Map.class);
            if (user != null && user.get("displayName") != null) return user.get("displayName").toString();
            if (user != null && user.get("username") != null) return user.get("username").toString();
        } catch (Exception ignored) {}
        return userId.toString();
    }
}
