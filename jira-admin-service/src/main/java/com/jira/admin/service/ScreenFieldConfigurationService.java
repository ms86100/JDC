package com.jira.admin.service;

import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Screen/Field Configuration Enforcement Service.
 * Enforces field visibility, required fields, and read-only fields
 * based on Jira DC field configuration schemes at runtime.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenFieldConfigurationService {

    private final FieldConfigurationRepository fieldConfigurationRepository;
    private final FieldConfigurationItemRepository fieldConfigurationItemRepository;
    private final ScreenSchemeRepository screenSchemeRepository;
    private final ScreenRepository screenRepository;
    private final IssueTypeScreenSchemeRepository issueTypeScreenSchemeRepository;
    private final ProjectRepository projectRepository;

    /**
     * Get field configuration for a specific issue type and project.
     * Resolution: IssueTypeScreenScheme → FieldConfigurationScheme → FieldConfiguration
     */
    @Transactional(readOnly = true)
    public FieldConfigurationResult getFieldConfiguration(String projectId, String issueTypeId) {
        // Get project's field configuration scheme (simplified - would normally join through multiple tables)
        Optional<ProjectEntity> project = projectRepository.findById(projectId);
        if (project.isEmpty() || project.get().getFieldConfigurationScheme() == null) {
            // Return default configuration
            return getDefaultFieldConfiguration();
        }

        // For now, return a basic configuration based on project settings
        return buildFieldConfigurationResult(project.get().getFieldConfigurationScheme());
    }

    /**
     * Check if a field should be shown for an issue.
     */
    @Transactional(readOnly = true)
    public boolean isFieldShown(String projectId, String issueTypeId, String fieldKey) {
        FieldConfigurationResult config = getFieldConfiguration(projectId, issueTypeId);
        FieldConfigurationResult.FieldConfig fieldConfig = config.getFields().get(fieldKey);

        // If field not in config, show by default
        if (fieldConfig == null) {
            return true;
        }

        return fieldConfig.isShown();
    }

    /**
     * Check if a field is required for an issue.
     */
    @Transactional(readOnly = true)
    public boolean isFieldRequired(String projectId, String issueTypeId, String fieldKey) {
        FieldConfigurationResult config = getFieldConfiguration(projectId, issueTypeId);
        FieldConfigurationResult.FieldConfig fieldConfig = config.getFields().get(fieldKey);

        // If field not in config, not required by default
        if (fieldConfig == null) {
            return false;
        }

        return fieldConfig.isRequired();
    }

    /**
     * Check if a field is read-only for an issue.
     */
    @Transactional(readOnly = true)
    public boolean isFieldReadOnly(String projectId, String issueTypeId, String fieldKey) {
        FieldConfigurationResult config = getFieldConfiguration(projectId, issueTypeId);
        FieldConfigurationResult.FieldConfig fieldConfig = config.getFields().get(fieldKey);

        // If field not in config, editable by default
        if (fieldConfig == null) {
            return false;
        }

        return fieldConfig.isReadOnly();
    }

    /**
     * Validate that all required fields are present in a request.
     * Returns list of missing required fields.
     */
    @Transactional(readOnly = true)
    public List<String> validateRequiredFields(String projectId, String issueTypeId, Map<String, Object> fieldValues) {
        List<String> missingFields = new ArrayList<>();

        FieldConfigurationResult config = getFieldConfiguration(projectId, issueTypeId);

        for (Map.Entry<String, FieldConfigurationResult.FieldConfig> entry : config.getFields().entrySet()) {
            String fieldKey = entry.getKey();
            FieldConfigurationResult.FieldConfig fieldConfig = entry.getValue();

            if (fieldConfig.isRequired() && fieldConfig.isShown()) {
                // Check if field has a value
                Object value = fieldValues.get(fieldKey);
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    missingFields.add(fieldKey);
                }
            }
        }

        return missingFields;
    }

    /**
     * Get list of visible fields for an issue type.
     */
    @Transactional(readOnly = true)
    public List<String> getVisibleFields(String projectId, String issueTypeId) {
        FieldConfigurationResult config = getFieldConfiguration(projectId, issueTypeId);
        List<String> visibleFields = new ArrayList<>();

        for (Map.Entry<String, FieldConfigurationResult.FieldConfig> entry : config.getFields().entrySet()) {
            if (entry.getValue().isShown()) {
                visibleFields.add(entry.getKey());
            }
        }

        return visibleFields;
    }

    /**
     * Filter a map of field values to only include visible fields.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> filterVisibleFields(String projectId, String issueTypeId, Map<String, Object> allFields) {
        Map<String, Object> visibleFields = new HashMap<>();

        for (Map.Entry<String, Object> entry : allFields.entrySet()) {
            if (isFieldShown(projectId, issueTypeId, entry.getKey())) {
                visibleFields.put(entry.getKey(), entry.getValue());
            }
        }

        return visibleFields;
    }

    /**
     * Get screen configuration (tabs and fields) for a screen.
     */
    @Transactional(readOnly = true)
    public ScreenConfigResult getScreenConfig(String screenId, String operation) {
        // operation: "create", "edit", "view"
        ScreenEntity screen = screenRepository.findById(screenId).orElse(null);
        if (screen == null) {
            return getDefaultScreenConfig();
        }

        // Build screen configuration based on screen tabs
        ScreenConfigResult result = new ScreenConfigResult();
        result.setScreenId(screenId);
        result.setScreenName(screen.getName());
        result.setScreenType(screen.getScreenType());

        // Return default tab structure for now
        // Full implementation would use screen.tabs
        result.setTabs(buildDefaultScreenTabs(operation));

        return result;
    }

    // ===== Private helper methods =====

    private FieldConfigurationResult getDefaultFieldConfiguration() {
        FieldConfigurationResult result = new FieldConfigurationResult();
        result.setConfigurationId("default");
        result.setConfigurationName("Default Field Configuration");

        Map<String, FieldConfigurationResult.FieldConfig> fields = new HashMap<>();

        // Standard Jira fields with default visibility/requirements
        String[][] standardFields = {
            {"summary", "true", "true", "false"},      // shown, required, readOnly
            {"description", "true", "false", "false"},
            {"priority", "true", "false", "false"},
            {"assignee", "true", "false", "false"},
            {"reporter", "true", "false", "false"},
            {"labels", "true", "false", "false"},
            {"security", "true", "false", "false"},
            {"duedate", "true", "false", "false"},
            {"components", "true", "false", "false"},
            {"fixVersions", "true", "false", "false"},
            {"affectsVersions", "true", "false", "false"},
            {"linkedIssues", "true", "false", "false"},
            {"attachment", "true", "false", "false"},
            {"comment", "true", "false", "false"},
            {"worklog", "true", "false", "false"},
            {"subtasks", "true", "false", "false"},
            {"epic", "true", "false", "false"},
            {"storyPoints", "true", "false", "false"},
            {"sprint", "true", "false", "false"},
            {"flagged", "true", "false", "false"}
        };

        for (String[] fieldDef : standardFields) {
            FieldConfigurationResult.FieldConfig fc = new FieldConfigurationResult.FieldConfig();
            fc.setFieldKey(fieldDef[0]);
            fc.setShown(Boolean.parseBoolean(fieldDef[1]));
            fc.setRequired(Boolean.parseBoolean(fieldDef[2]));
            fc.setReadOnly(Boolean.parseBoolean(fieldDef[3]));
            fields.put(fieldDef[0], fc);
        }

        result.setFields(fields);
        return result;
    }

    private FieldConfigurationResult buildFieldConfigurationResult(String schemeId) {
        // Build from database configuration
        FieldConfigurationResult result = new FieldConfigurationResult();
        result.setConfigurationId(schemeId);

        Map<String, FieldConfigurationResult.FieldConfig> fields = new HashMap<>();

        // Get configured items
        List<FieldConfigurationItemEntity> items = fieldConfigurationItemRepository
                .findByFieldConfigurationId(schemeId);

        for (FieldConfigurationItemEntity item : items) {
            FieldConfigurationResult.FieldConfig fc = new FieldConfigurationResult.FieldConfig();
            fc.setFieldKey(item.getFieldKey());
            fc.setShown(item.getIsShown() != null ? item.getIsShown() : true);
            fc.setRequired(item.getIsRequired() != null ? item.getIsRequired() : false);
            fc.setReadOnly(item.getIsReadOnly() != null ? item.getIsReadOnly() : false);
            fields.put(item.getFieldKey(), fc);
        }

        // Merge with defaults for any unspecified fields
        FieldConfigurationResult defaults = getDefaultFieldConfiguration();
        for (Map.Entry<String, FieldConfigurationResult.FieldConfig> entry : defaults.getFields().entrySet()) {
            if (!fields.containsKey(entry.getKey())) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }

        result.setFields(fields);
        return result;
    }

    private ScreenConfigResult getDefaultScreenConfig() {
        return buildScreenConfigResult("default", "Default Screen", "edit");
    }

    private ScreenConfigResult buildScreenConfigResult(String screenId, String screenName, String operation) {
        ScreenConfigResult result = new ScreenConfigResult();
        result.setScreenId(screenId);
        result.setScreenName(screenName);
        result.setTabs(buildDefaultScreenTabs(operation));
        return result;
    }

    private List<ScreenConfigResult.ScreenTabConfig> buildDefaultScreenTabs(String operation) {
        List<ScreenConfigResult.ScreenTabConfig> tabs = new ArrayList<>();

        // Description tab
        ScreenConfigResult.ScreenTabConfig descTab = new ScreenConfigResult.ScreenTabConfig();
        descTab.setTabName("Description");
        descTab.setTabOrder(1);
        descTab.setFields(Arrays.asList("summary", "description", "environment"));
        tabs.add(descTab);

        // Details tab
        ScreenConfigResult.ScreenTabConfig detailsTab = new ScreenConfigResult.ScreenTabConfig();
        detailsTab.setTabName("Details");
        detailsTab.setTabOrder(2);
        if ("create".equals(operation)) {
            detailsTab.setFields(Arrays.asList("issuetype", "priority", "assignee", "reporter", "labels",
                "security", "duedate", "components", "fixVersions", "affectsVersions"));
        } else {
            detailsTab.setFields(Arrays.asList("issuetype", "priority", "assignee", "reporter", "labels",
                "security", "duedate", "components", "fixVersions", "affectsVersions",
                "linkedIssues", "subtasks", "attachment"));
        }
        tabs.add(detailsTab);

        // People tab (for edit/view)
        if (!"create".equals(operation)) {
            ScreenConfigResult.ScreenTabConfig peopleTab = new ScreenConfigResult.ScreenTabConfig();
            peopleTab.setTabName("People");
            peopleTab.setTabOrder(3);
            peopleTab.setFields(Arrays.asList("watcher", "vote", "comment"));
            tabs.add(peopleTab);
        }

        // Development tab (for edit/view)
        if (!"create".equals(operation)) {
            ScreenConfigResult.ScreenTabConfig devTab = new ScreenConfigResult.ScreenTabConfig();
            devTab.setTabName("Development");
            devTab.setTabOrder(4);
            devTab.setFields(Arrays.asList("sprint", "epic", "storyPoints", "flagged", "worklog"));
            tabs.add(devTab);
        }

        // Activity tab (for view only)
        if ("view".equals(operation)) {
            ScreenConfigResult.ScreenTabConfig activityTab = new ScreenConfigResult.ScreenTabConfig();
            activityTab.setTabName("Activity");
            activityTab.setTabOrder(5);
            activityTab.setFields(Arrays.asList("activity"));
            tabs.add(activityTab);
        }

        return tabs;
    }

    // ===== Inner classes for results =====

    @lombok.Data
    public static class FieldConfigurationResult {
        private String configurationId;
        private String configurationName;
        private Map<String, FieldConfig> fields = new HashMap<>();

        @lombok.Data
        public static class FieldConfig {
            private String fieldKey;
            private boolean shown = true;
            private boolean required = false;
            private boolean readOnly = false;
            private String renderer;
        }
    }

    @lombok.Data
    public static class ScreenConfigResult {
        private String screenId;
        private String screenName;
        private String screenType;
        private List<ScreenTabConfig> tabs = new ArrayList<>();

        @lombok.Data
        public static class ScreenTabConfig {
            private String tabName;
            private int tabOrder;
            private List<String> fields = new ArrayList<>();
        }
    }
}