package com.jira.issue.customfield;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultiUserPickerFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    @Value("${user.service.url:http://jira-user-service:8082}")
    private String userServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getType() { return "multiuserpicker"; }

    @Override
    public String getDisplayName() { return "User Picker (Multiple Users)"; }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        List<String> userIds;
        if (value instanceof List) {
            userIds = ((List<Object>) value).stream().map(Object::toString).collect(Collectors.toList());
        } else {
            userIds = List.of(value.toString());
        }
        if (userIds.isEmpty()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        for (String uid : userIds) {
            try {
                UUID.fromString(uid);
            } catch (IllegalArgumentException e) {
                return ValidationResult.error("Invalid user ID format: " + uid);
            }
        }
        return ValidationResult.success(userIds);
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
}
