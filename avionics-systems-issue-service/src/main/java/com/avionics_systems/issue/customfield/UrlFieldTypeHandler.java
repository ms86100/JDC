package com.avionics_systems.issue.customfield;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * URL field type handler for web URL custom fields.
 * F7-US001: Custom Field Types
 */
@Component
public class UrlFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private static final String URL_PATTERN =
            "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$";

    @Override
    public String getType() {
        return "url";
    }

    @Override
    public String getDisplayName() {
        return "URL Field";
    }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null) {
            if (Boolean.TRUE.equals(config.get("required"))) {
                return ValidationResult.error("This field is required");
            }
            return ValidationResult.success();
        }

        String url = value.toString().trim();

        if (url.isEmpty()) {
            if (Boolean.TRUE.equals(config.get("required"))) {
                return ValidationResult.error("This field is required");
            }
            return ValidationResult.success();
        }

        // Auto-prepend https:// if no protocol specified
        String processedUrl = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            processedUrl = "https://" + url;
        }

        // Validate URL format
        if (!processedUrl.matches(URL_PATTERN)) {
            return ValidationResult.error("Invalid URL format");
        }

        return ValidationResult.success(processedUrl);
    }

    @Override
    public String renderForDisplay(Object value, Map<String, Object> config) {
        if (value == null || value.toString().isBlank()) {
            return "<span class=\"cf-empty\">-</span>";
        }

        String url = value.toString();
        String displayText = getDisplayText(config, url);

        return String.format(
                "<a href=\"%s\" class=\"cf-url\" target=\"_blank\" rel=\"noopener noreferrer\">%s</a>",
                escapeHtml(url),
                escapeHtml(displayText)
        );
    }

    @Override
    public String renderForEdit(Object value, Map<String, Object> config) {
        String inputId = getInputId(config);
        String inputName = getInputName(config);
        String currentValue = value != null ? escapeHtml(value.toString()) : "";
        String placeholder = getPlaceholder(config);
        if (placeholder.isEmpty()) {
            placeholder = "https://example.com";
        }
        String cssClass = getCssClass(config) + " cf-url-input";
        boolean required = isRequired(config);

        return String.format(
                "<input type=\"url\" id=\"%s\" name=\"%s\" value=\"%s\" placeholder=\"%s\" class=\"%s\" %s />",
                inputId, inputName, currentValue, placeholder, cssClass, required ? "required" : ""
        );
    }

    private String getDisplayText(Map<String, Object> config, String url) {
        if (config != null && config.containsKey("displayText")) {
            return config.get("displayText").toString();
        }

        // Extract hostname for display
        try {
            String displayUrl = url.replaceFirst("^https?://", "");
            int slashIndex = displayUrl.indexOf('/');
            return slashIndex > 0 ? displayUrl.substring(0, slashIndex) : displayUrl;
        } catch (Exception e) {
            return url;
        }
    }
}