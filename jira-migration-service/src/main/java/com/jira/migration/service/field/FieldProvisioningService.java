package com.jira.migration.service.field;

import com.jira.migration.entity.field.*;
import com.jira.migration.repository.field.*;
import com.jira.migration.service.field.FieldDiscoveryService.DiscoveredField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for auto-provisioning missing field definitions.
 * Creates DB schema mappings, UI renderer mappings, search indexing configs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldProvisioningService {

    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CustomFieldContextRepository customFieldContextRepository;
    private final CustomFieldOptionRepository customFieldOptionRepository;
    private final IssueFieldValueRepository issueFieldValueRepository;
    private final PluginFieldRegistryRepository pluginFieldRegistryRepository;
    private final FieldScreenConfigurationService fieldScreenConfigurationService;

    private static final Map<String, FieldDefinition.FieldType> DEFAULT_TYPE_MAP = Map.ofEntries(
            Map.entry("summary", FieldDefinition.FieldType.TEXT),
            Map.entry("description", FieldDefinition.FieldType.RICHTEXT),
            Map.entry("environment", FieldDefinition.FieldType.TEXTAREA),
            Map.entry("issue_type", FieldDefinition.FieldType.ISSUE_TYPE),
            Map.entry("status", FieldDefinition.FieldType.STATUS),
            Map.entry("priority", FieldDefinition.FieldType.PRIORITY),
            Map.entry("resolution", FieldDefinition.FieldType.RESOLUTION),
            Map.entry("assignee", FieldDefinition.FieldType.USER),
            Map.entry("reporter", FieldDefinition.FieldType.USER),
            Map.entry("creator", FieldDefinition.FieldType.USER),
            Map.entry("labels", FieldDefinition.FieldType.LABEL),
            Map.entry("components", FieldDefinition.FieldType.COMPONENT),
            Map.entry("affects_versions", FieldDefinition.FieldType.VERSION),
            Map.entry("fix_versions", FieldDefinition.FieldType.VERSION),
            Map.entry("security_level", FieldDefinition.FieldType.SECURITY_LEVEL),
            Map.entry("epic_link", FieldDefinition.FieldType.EPIC),
            Map.entry("epic_name", FieldDefinition.FieldType.TEXT),
            Map.entry("epic_color", FieldDefinition.FieldType.TEXT),
            Map.entry("story_points", FieldDefinition.FieldType.NUMBER),
            Map.entry("sprint", FieldDefinition.FieldType.SPRINT),
            Map.entry("rank", FieldDefinition.FieldType.TEXT),
            Map.entry("original_estimate", FieldDefinition.FieldType.DURATION),
            Map.entry("remaining_estimate", FieldDefinition.FieldType.DURATION),
            Map.entry("time_spent", FieldDefinition.FieldType.DURATION),
            Map.entry("votes", FieldDefinition.FieldType.VOTES),
            Map.entry("watchers", FieldDefinition.FieldType.WATCHERS),
            Map.entry("parent", FieldDefinition.FieldType.PARENT_ISSUE),
            Map.entry("due_date", FieldDefinition.FieldType.DATE)
    );

    private static final Map<String, FieldDefinition.ScreenRegion> DEFAULT_REGION_MAP = Map.ofEntries(
            Map.entry("description", FieldDefinition.ScreenRegion.LEFT_DESCRIPTION),
            Map.entry("environment", FieldDefinition.ScreenRegion.LEFT_DESCRIPTION),
            Map.entry("assignee", FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE),
            Map.entry("reporter", FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE),
            Map.entry("creator", FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE),
            Map.entry("votes", FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE),
            Map.entry("watchers", FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE),
            Map.entry("priority", FieldDefinition.ScreenRegion.SIDEBAR_DETAILS),
            Map.entry("resolution", FieldDefinition.ScreenRegion.SIDEBAR_DETAILS),
            Map.entry("labels", FieldDefinition.ScreenRegion.SIDEBAR_DETAILS),
            Map.entry("components", FieldDefinition.ScreenRegion.SIDEBAR_DETAILS),
            Map.entry("security_level", FieldDefinition.ScreenRegion.SIDEBAR_DETAILS),
            Map.entry("original_estimate", FieldDefinition.ScreenRegion.SIDEBAR_TIME),
            Map.entry("remaining_estimate", FieldDefinition.ScreenRegion.SIDEBAR_TIME),
            Map.entry("time_spent", FieldDefinition.ScreenRegion.SIDEBAR_TIME),
            Map.entry("sprint", FieldDefinition.ScreenRegion.SIDEBAR_AGILE),
            Map.entry("epic_link", FieldDefinition.ScreenRegion.SIDEBAR_AGILE),
            Map.entry("epic_name", FieldDefinition.ScreenRegion.SIDEBAR_AGILE),
            Map.entry("story_points", FieldDefinition.ScreenRegion.SIDEBAR_AGILE),
            Map.entry("rank", FieldDefinition.ScreenRegion.SIDEBAR_AGILE),
            Map.entry("due_date", FieldDefinition.ScreenRegion.SIDEBAR_DATES),
            Map.entry("affects_versions", FieldDefinition.ScreenRegion.SIDEBAR_VERSIONS),
            Map.entry("fix_versions", FieldDefinition.ScreenRegion.SIDEBAR_VERSIONS)
    );

    private static final Map<String, FieldDefinition.FieldRenderer> DEFAULT_RENDERER_MAP = Map.ofEntries(
            Map.entry("summary", FieldDefinition.FieldRenderer.TEXT),
            Map.entry("description", FieldDefinition.FieldRenderer.RICHTEXT),
            Map.entry("environment", FieldDefinition.FieldRenderer.TEXTAREA),
            Map.entry("issue_type", FieldDefinition.FieldRenderer.SELECT),
            Map.entry("status", FieldDefinition.FieldRenderer.SELECT),
            Map.entry("priority", FieldDefinition.FieldRenderer.SELECT),
            Map.entry("resolution", FieldDefinition.FieldRenderer.SELECT),
            Map.entry("assignee", FieldDefinition.FieldRenderer.USER_PICKER),
            Map.entry("reporter", FieldDefinition.FieldRenderer.USER_PICKER),
            Map.entry("creator", FieldDefinition.FieldRenderer.USER_PICKER),
            Map.entry("labels", FieldDefinition.FieldRenderer.LABEL_EDITOR),
            Map.entry("components", FieldDefinition.FieldRenderer.MULTI_SELECT),
            Map.entry("affects_versions", FieldDefinition.FieldRenderer.MULTI_SELECT),
            Map.entry("fix_versions", FieldDefinition.FieldRenderer.MULTI_SELECT),
            Map.entry("security_level", FieldDefinition.FieldRenderer.SECURITY_LEVEL),
            Map.entry("epic_link", FieldDefinition.FieldRenderer.EPIC_LINK),
            Map.entry("story_points", FieldDefinition.FieldRenderer.NUMBER),
            Map.entry("sprint", FieldDefinition.FieldRenderer.SPRINT_SELECTOR),
            Map.entry("original_estimate", FieldDefinition.FieldRenderer.DURATION),
            Map.entry("remaining_estimate", FieldDefinition.FieldRenderer.DURATION),
            Map.entry("time_spent", FieldDefinition.FieldRenderer.DURATION),
            Map.entry("votes", FieldDefinition.FieldRenderer.VOTES),
            Map.entry("watchers", FieldDefinition.FieldRenderer.WATCHERS),
            Map.entry("due_date", FieldDefinition.FieldRenderer.DATETIME_PICKER)
    );

    public record ProvisioningResult(
            List<FieldDefinition> provisionedFields,
            List<FieldDefinition> existingFields,
            List<FieldDefinition> failedFields,
            Map<String, String> fieldKeyMapping,
            int totalProvisioned
    ) {}

    public record SingleProvisioningResult(
            FieldDefinition fieldDefinition,
            boolean isNew,
            boolean success,
            String errorMessage
    ) {}

    @Transactional
    public ProvisioningResult provisionFields(List<DiscoveredField> discoveredFields, UUID userId) {
        log.info("Starting field provisioning for {} fields", discoveredFields.size());

        List<FieldDefinition> provisioned = new ArrayList<>();
        List<FieldDefinition> existing = new ArrayList<>();
        List<FieldDefinition> failed = new ArrayList<>();
        Map<String, String> keyMapping = new HashMap<>();

        for (DiscoveredField discovered : discoveredFields) {
            try {
                SingleProvisioningResult result = provisionField(discovered, userId);

                if (result.success()) {
                    provisioned.add(result.fieldDefinition());
                    keyMapping.put(discovered.sourceKey(), result.fieldDefinition().getFieldKey());
                } else if (result.isNew()) {
                    failed.add(result.fieldDefinition());
                } else {
                    existing.add(result.fieldDefinition());
                    keyMapping.put(discovered.sourceKey(), result.fieldDefinition().getFieldKey());
                }
            } catch (Exception e) {
                log.error("Failed to provision field: {}", discovered.sourceKey(), e);
                failed.add(null);
            }
        }

        log.info("Provisioning complete: {} provisioned, {} existing, {} failed",
                provisioned.size(), existing.size(), failed.size());

        return new ProvisioningResult(provisioned, existing, failed, keyMapping, provisioned.size());
    }

    @Transactional
    public SingleProvisioningResult provisionField(DiscoveredField discovered, UUID userId) {
        String fieldKey = discovered.normalizedKey();

        // First, try to find existing field definition
        Optional<FieldDefinition> existing = fieldDefinitionRepository.findByFieldKey(fieldKey);
        if (existing.isPresent()) {
            return new SingleProvisioningResult(existing.get(), false, true, null);
        }

        // Attempt to create the field definition with conflict handling
        // Use a retry mechanism to handle race conditions where another thread
        // creates the same field between our check and save
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                FieldDefinition fieldDef = createFieldDefinition(discovered, userId);
                FieldDefinition saved = fieldDefinitionRepository.save(fieldDef);

                if (saved.getFieldType() == FieldDefinition.FieldType.CUSTOM ||
                        discovered.category() == FieldDiscoveryService.FieldCategory.CUSTOM ||
                        discovered.category() == FieldDiscoveryService.FieldCategory.PLUGIN ||
                        discovered.category() == FieldDiscoveryService.FieldCategory.MARKETPLACE) {
                    createCustomFieldFromDefinition(saved);
                }

                if (discovered.category() == FieldDiscoveryService.FieldCategory.PLUGIN ||
                        discovered.category() == FieldDiscoveryService.FieldCategory.MARKETPLACE) {
                    createPluginFieldRegistry(saved, discovered);
                }

                log.info("Provisioned new field: {} ({})", saved.getFieldKey(), saved.getId());
                return new SingleProvisioningResult(saved, true, true, null);

            } catch (Exception e) {
                // Check if this is a constraint violation (duplicate key)
                if (isDuplicateKeyException(e)) {
                    retryCount++;
                    log.warn("Duplicate field key detected for '{}', retrying ({}/{})", fieldKey, retryCount, maxRetries);

                    if (retryCount >= maxRetries) {
                        // Fetch the existing record instead of failing
                        Optional<FieldDefinition> existingAfterRetry = fieldDefinitionRepository.findByFieldKey(fieldKey);
                        if (existingAfterRetry.isPresent()) {
                            log.info("Field '{}' was created by another thread, using existing definition", fieldKey);
                            return new SingleProvisioningResult(existingAfterRetry.get(), false, true, null);
                        }
                    }
                    // Small delay before retry
                    try {
                        Thread.sleep(50 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // Not a duplicate key error, rethrow
                    throw e;
                }
            }
        }

        // This should not be reached, but safety fallback
        log.error("Failed to provision field '{}' after {} retries", fieldKey, maxRetries);
        return new SingleProvisioningResult(null, true, false, "Failed to provision field after retries");
    }

    /**
     * Check if exception is caused by duplicate key constraint violation.
     * Handles PostgreSQL-specific error codes.
     */
    private boolean isDuplicateKeyException(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;

        // PostgreSQL duplicate key error codes
        return message.contains("duplicate key") ||
               message.contains("unique constraint") ||
               message.contains("23505") || // PostgreSQL unique_violation code
               message.contains("UQ_"); // Hibernate unique constraint naming pattern
    }

    @Transactional
    public FieldDefinition provisionCustomField(String name, String type, UUID userId) {
        String fieldKey = generateFieldKey(name);

        Optional<FieldDefinition> existing = fieldDefinitionRepository.findByFieldKey(fieldKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        FieldDefinition.FieldType fieldType = mapCustomFieldType(type);
        FieldDefinition.FieldRenderer renderer = mapTypeToRenderer(fieldType);

        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                FieldDefinition fieldDef = FieldDefinition.builder()
                        .fieldKey(fieldKey)
                        .displayName(name)
                        .fieldType(fieldType)
                        .renderer(renderer)
                        .screenRegion(FieldDefinition.ScreenRegion.SIDEBAR)
                        .custom(true)
                        .builtIn(false)
                        .searchable(true)
                        .sortable(true)
                        .filterable(true)
                        .required(false)
                        .version(1)
                        .createdBy(userId)
                        .build();

                FieldDefinition saved = fieldDefinitionRepository.save(fieldDef);
                createCustomFieldFromDefinition(saved);

                return saved;

            } catch (Exception e) {
                if (isDuplicateKeyException(e)) {
                    retryCount++;
                    log.warn("Duplicate custom field key '{}', retrying ({}/{})", fieldKey, retryCount, maxRetries);

                    if (retryCount >= maxRetries) {
                        return fieldDefinitionRepository.findByFieldKey(fieldKey)
                                .orElseThrow(() -> new RuntimeException("Failed to provision custom field: " + fieldKey));
                    }
                    try {
                        Thread.sleep(50 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw e;
                }
            }
        }

        throw new RuntimeException("Failed to provision custom field after retries: " + fieldKey);
    }

    private FieldDefinition createFieldDefinition(DiscoveredField discovered, UUID userId) {
        String fieldKey = discovered.normalizedKey();
        String displayName = generateDisplayName(fieldKey);

        FieldDefinition.FieldType fieldType = DEFAULT_TYPE_MAP.getOrDefault(
                fieldKey, discovered.suggestedType());
        FieldDefinition.FieldRenderer renderer = DEFAULT_RENDERER_MAP.getOrDefault(
                fieldKey, mapTypeToRenderer(fieldType));
        FieldDefinition.ScreenRegion screenRegion = DEFAULT_REGION_MAP.getOrDefault(
                fieldKey, discovered.suggestedRegion());

        Map<String, Object> schemaDefinition = buildSchemaDefinition(fieldType);
        Map<String, Object> visibilityRules = buildVisibilityRules(discovered.category());
        Map<String, Object> validationRules = buildValidationRules(fieldType);

        return FieldDefinition.builder()
                .fieldKey(fieldKey)
                .displayName(displayName)
                .description("Auto-provisioned field from import: " + discovered.sourceKey())
                .fieldType(fieldType)
                .renderer(renderer)
                .screenRegion(screenRegion)
                .pluginSource(determinePluginSource(discovered))
                .custom(discovered.category() == FieldDiscoveryService.FieldCategory.CUSTOM)
                .builtIn(false)
                .searchable(true)
                .sortable(true)
                .filterable(true)
                .required(false)
                .schemaDefinition(schemaDefinition)
                .visibilityRules(visibilityRules)
                .validationRules(validationRules)
                .version(1)
                .createdBy(userId)
                .build();
    }

    private void createCustomFieldFromDefinition(FieldDefinition fieldDef) {
        String customFieldType = mapFieldTypeToCustomFieldType(fieldDef.getFieldType());

        CustomFieldDefinition customDef = CustomFieldDefinition.builder()
                .name(fieldDef.getDisplayName())
                .description(fieldDef.getDescription())
                .type(customFieldType)
                .searcherKey(getSearcherKey(fieldDef.getFieldType()))
                .rendererKey(getRendererKey(fieldDef.getRenderer()))
                .fieldKey(fieldDef.getFieldKey())
                .enabled(true)
                .searchable(fieldDef.getSearchable())
                .navigable(true)
                .clauseNames(generateClauseNames(fieldDef.getFieldKey()))
                .createdBy(fieldDef.getCreatedBy())
                .build();

        CustomFieldDefinition saved = customFieldDefinitionRepository.save(customDef);

        CustomFieldContext defaultContext = CustomFieldContext.builder()
                .customFieldId(saved.getId())
                .name("Default Context")
                .allProjects(true)
                .enabled(true)
                .displayOrder(0)
                .build();

        customFieldContextRepository.save(defaultContext);
        fieldScreenConfigurationService.ensureFieldVisibleOnScreen(fieldDef.getFieldKey(), null);
    }

    private void createPluginFieldRegistry(FieldDefinition fieldDef, DiscoveredField discovered) {
        String pluginKey = determinePluginKey(discovered);

        if (pluginKey == null) return;

        PluginFieldRegistry registry = PluginFieldRegistry.builder()
                .pluginKey(pluginKey)
                .pluginName(formatPluginName(pluginKey))
                .fieldKey(discovered.sourceKey())
                .fieldType(fieldDef.getFieldType().name())
                .jiraFieldKey(fieldDef.getFieldKey())
                .schemaMapping(Map.of(
                        "type", fieldDef.getFieldType().name(),
                        "renderer", fieldDef.getRenderer().name()
                ))
                .importMapping(Map.of(
                        "sourceKey", discovered.sourceKey(),
                        "normalizedKey", discovered.normalizedKey()
                ))
                .exportMapping(Map.of(
                        "targetKey", fieldDef.getFieldKey()
                ))
                .searchable(fieldDef.getSearchable())
                .navigable(fieldDef.getNavigable() != null ? fieldDef.getNavigable() : true)
                .fieldDefinitionId(fieldDef.getId())
                .enabled(true)
                .deployed(true)
                .build();

        pluginFieldRegistryRepository.save(registry);
        log.info("Created plugin field registry: {} -> {}", pluginKey, discovered.sourceKey());
    }

    private String generateFieldKey(String name) {
        return "customfield_" + name.toLowerCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    private String generateDisplayName(String fieldKey) {
        String[] words = fieldKey.split("_");
        StringBuilder display = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                display.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    display.append(word.substring(1));
                }
                display.append(" ");
            }
        }

        return display.toString().trim();
    }

    private Map<String, Object> buildSchemaDefinition(FieldDefinition.FieldType fieldType) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", fieldType.name());
        schema.put("searchable", true);
        schema.put("sortable", true);

        switch (fieldType) {
            case NUMBER, DURATION -> {
                schema.put("precision", "integer");
            }
            case TEXT, TEXTAREA -> {
                schema.put("maxLength", 5000);
            }
            case DATE -> {
                schema.put("format", "yyyy-MM-dd");
            }
            case DATETIME -> {
                schema.put("format", "yyyy-MM-dd'T'HH:mm:ss");
            }
        }

        return schema;
    }

    private Map<String, Object> buildVisibilityRules(FieldDiscoveryService.FieldCategory category) {
        Map<String, Object> rules = new HashMap<>();
        rules.put("category", category.name());

        switch (category) {
            case PLUGIN, MARKETPLACE -> {
                rules.put("requiresPlugin", true);
            }
            case AGILE -> {
                rules.put("requiresAgileBoard", true);
            }
            default -> {
                rules.put("alwaysVisible", true);
            }
        }

        return rules;
    }

    private Map<String, Object> buildValidationRules(FieldDefinition.FieldType fieldType) {
        Map<String, Object> rules = new HashMap<>();

        switch (fieldType) {
            case TEXT -> {
                rules.put("maxLength", 5000);
            }
            case TEXTAREA -> {
                rules.put("maxLength", 50000);
            }
            case NUMBER, STORY_POINTS -> {
                rules.put("min", 0);
                rules.put("max", 10000);
            }
            case EMAIL -> {
                rules.put("pattern", "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
            }
            case URL -> {
                rules.put("pattern", "^https?://.*");
            }
        }

        return rules;
    }

    private String determinePluginSource(DiscoveredField discovered) {
        if (discovered.category() == FieldDiscoveryService.FieldCategory.PLUGIN ||
                discovered.category() == FieldDiscoveryService.FieldCategory.MARKETPLACE) {
            return discovered.sourceKey().split("_")[0];
        }
        return null;
    }

    private String determinePluginKey(DiscoveredField discovered) {
        String source = discovered.sourceKey();

        if (source.startsWith("tempo_")) return "tempo";
        if (source.startsWith("xray_")) return "xray";
        if (source.startsWith("zephyr_")) return "zephyr";
        if (source.startsWith("structure_")) return "structure";
        if (source.startsWith("bigpicture_")) return "bigpicture";

        return null;
    }

    private String formatPluginName(String pluginKey) {
        return Arrays.stream(pluginKey.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .reduce("", (a, b) -> a + " " + b)
                .trim();
    }

    private FieldDefinition.FieldType mapCustomFieldType(String type) {
        if (type == null) return FieldDefinition.FieldType.CUSTOM;

        return switch (type.toLowerCase()) {
            case "textfield", "text" -> FieldDefinition.FieldType.TEXT;
            case "textarea", "text area" -> FieldDefinition.FieldType.TEXTAREA;
            case "datepicker", "date" -> FieldDefinition.FieldType.DATE;
            case "datetime", "datetime picker" -> FieldDefinition.FieldType.DATETIME;
            case "number", "numeric" -> FieldDefinition.FieldType.NUMBER;
            case "select", "dropdown" -> FieldDefinition.FieldType.SINGLE_SELECT;
            case "multiselect", "multi-select" -> FieldDefinition.FieldType.MULTI_SELECT;
            case "radiobuttons", "radio" -> FieldDefinition.FieldType.RADIO;
            case "checkbox" -> FieldDefinition.FieldType.CHECKBOX;
            case "userpicker", "user" -> FieldDefinition.FieldType.USER;
            case "multiuserpicker", "multi-user" -> FieldDefinition.FieldType.USER;
            case "projectpicker", "project" -> FieldDefinition.FieldType.PROJECT;
            case "versionpicker", "version" -> FieldDefinition.FieldType.VERSION;
            case "labels" -> FieldDefinition.FieldType.LABEL;
            case "url", "link" -> FieldDefinition.FieldType.URL;
            case "email" -> FieldDefinition.FieldType.EMAIL;
            case "float", "decimal" -> FieldDefinition.FieldType.NUMBER;
            default -> FieldDefinition.FieldType.CUSTOM;
        };
    }

    private FieldDefinition.FieldRenderer mapTypeToRenderer(FieldDefinition.FieldType fieldType) {
        return switch (fieldType) {
            case TEXT -> FieldDefinition.FieldRenderer.TEXT;
            case TEXTAREA -> FieldDefinition.FieldRenderer.TEXTAREA;
            case RICHTEXT -> FieldDefinition.FieldRenderer.RICHTEXT;
            case NUMBER -> FieldDefinition.FieldRenderer.NUMBER;
            case DATE, DATETIME -> FieldDefinition.FieldRenderer.DATETIME_PICKER;
            case SINGLE_SELECT -> FieldDefinition.FieldRenderer.SELECT;
            case MULTI_SELECT -> FieldDefinition.FieldRenderer.MULTI_SELECT;
            case CHECKBOX -> FieldDefinition.FieldRenderer.CHECKBOX;
            case RADIO -> FieldDefinition.FieldRenderer.RADIO;
            case USER -> FieldDefinition.FieldRenderer.USER_PICKER;
            case GROUP -> FieldDefinition.FieldRenderer.GROUP_PICKER;
            case PROJECT -> FieldDefinition.FieldRenderer.PROJECT_PICKER;
            case LABEL -> FieldDefinition.FieldRenderer.LABEL_EDITOR;
            case VERSION -> FieldDefinition.FieldRenderer.MULTI_SELECT;
            case SECURITY_LEVEL -> FieldDefinition.FieldRenderer.SECURITY_LEVEL;
            case SPRINT -> FieldDefinition.FieldRenderer.SPRINT_SELECTOR;
            case EPIC -> FieldDefinition.FieldRenderer.EPIC_LINK;
            case VOTES -> FieldDefinition.FieldRenderer.VOTES;
            case WATCHERS -> FieldDefinition.FieldRenderer.WATCHERS;
            default -> FieldDefinition.FieldRenderer.TEXT;
        };
    }

    private String mapFieldTypeToCustomFieldType(FieldDefinition.FieldType fieldType) {
        return switch (fieldType) {
            case TEXT -> "com.atlassian.jira.plugin.system.customfieldtypes:textfield";
            case TEXTAREA -> "com.atlassian.jira.plugin.system.customfieldtypes:textarea";
            case RICHTEXT -> "com.atlassian.jira.plugin.system.customfieldtypes:textarea";
            case DATE -> "com.atlassian.jira.plugin.system.customfieldtypes:datepicker";
            case DATETIME -> "com.atlassian.jira.plugin.system.customfieldtypes:datetime";
            case NUMBER -> "com.atlassian.jira.plugin.system.customfieldtypes:number";
            case SINGLE_SELECT -> "com.atlassian.jira.plugin.system.customfieldtypes:select";
            case MULTI_SELECT -> "com.atlassian.jira.plugin.system.customfieldtypes:multiselect";
            case RADIO -> "com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons";
            case CHECKBOX -> "com.atlassian.jira.plugin.system.customfieldtypes:checkbox";
            case USER -> "com.atlassian.jira.plugin.system.customfieldtypes:userpicker";
            case PROJECT -> "com.atlassian.jira.plugin.system.customfieldtypes:projectpicker";
            case VERSION -> "com.atlassian.jira.plugin.system.customfieldtypes:versionpicker";
            case LABEL -> "com.atlassian.jira.plugin.system.customfieldtypes:labels";
            case URL -> "com.atlassian.jira.plugin.system.customfieldtypes:url";
            case EMAIL -> "com.atlassian.jira.plugin.system.customfieldtypes:email";
            case FLOAT -> "com.atlassian.jira.plugin.system.customfieldtypes:float";
            default -> "com.atlassian.jira.plugin.system.customfieldtypes:textfield";
        };
    }

    private String getSearcherKey(FieldDefinition.FieldType fieldType) {
        return switch (fieldType) {
            case TEXT, TEXTAREA -> "com.atlassian.jira.plugin.system.customfieldtypes:textsearcher";
            case NUMBER, FLOAT -> "com.atlassian.jira.plugin.system.customfieldtypes:numbersearcher";
            case DATE, DATETIME -> "com.atlassian.jira.plugin.system.customfieldtypes:datesearcher";
            case SINGLE_SELECT, MULTI_SELECT -> "com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher";
            case USER -> "com.atlassian.jira.plugin.system.customfieldtypes:usernamesearcher";
            case PROJECT -> "com.atlassian.jira.plugin.system.customfieldtypes:projectsearcher";
            case VERSION -> "com.atlassian.jira.plugin.system.customfieldtypes:versionsearcher";
            case LABEL -> "com.atlassian.jira.plugin.system.customfieldtypes:labelsearcher";
            default -> "com.atlassian.jira.plugin.system.customfieldtypes:textsearcher";
        };
    }

    private String getRendererKey(FieldDefinition.FieldRenderer renderer) {
        return switch (renderer) {
            case TEXT -> "com.atlassian.jira.plugin.system.customfieldtypes:textfield";
            case TEXTAREA -> "com.atlassian.jira.plugin.system.customfieldtypes:textarea";
            case SELECT -> "com.atlassian.jira.plugin.system.customfieldtypes:select";
            case MULTI_SELECT -> "com.atlassian.jira.plugin.system.customfieldtypes:multiselect";
            case NUMBER -> "com.atlassian.jira.plugin.system.customfieldtypes:number";
            default -> "com.atlassian.jira.plugin.system.customfieldtypes:textfield";
        };
    }

    private String[] generateClauseNames(String fieldKey) {
        return new String[]{
                fieldKey,
                fieldKey.replace("_", ""),
                fieldKey.replace("_", " ")
        };
    }

    @Transactional
    public void initializeBuiltInFields(UUID userId) {
        log.info("Initializing built-in field definitions");

        createBuiltInField("summary", "Summary", FieldDefinition.FieldType.TEXT,
                FieldDefinition.FieldRenderer.TEXT, FieldDefinition.ScreenRegion.HEADER, userId);
        createBuiltInField("description", "Description", FieldDefinition.FieldType.RICHTEXT,
                FieldDefinition.FieldRenderer.RICHTEXT, FieldDefinition.ScreenRegion.LEFT_DESCRIPTION, userId);
        createBuiltInField("environment", "Environment", FieldDefinition.FieldType.TEXTAREA,
                FieldDefinition.FieldRenderer.TEXTAREA, FieldDefinition.ScreenRegion.LEFT_DESCRIPTION, userId);
        createBuiltInField("issue_type", "Issue Type", FieldDefinition.FieldType.ISSUE_TYPE,
                FieldDefinition.FieldRenderer.SELECT, FieldDefinition.ScreenRegion.HEADER, userId);
        createBuiltInField("status", "Status", FieldDefinition.FieldType.STATUS,
                FieldDefinition.FieldRenderer.SELECT, FieldDefinition.ScreenRegion.HEADER, userId);
        createBuiltInField("priority", "Priority", FieldDefinition.FieldType.PRIORITY,
                FieldDefinition.FieldRenderer.SELECT, FieldDefinition.ScreenRegion.SIDEBAR_DETAILS, userId);
        createBuiltInField("resolution", "Resolution", FieldDefinition.FieldType.RESOLUTION,
                FieldDefinition.FieldRenderer.SELECT, FieldDefinition.ScreenRegion.SIDEBAR_DETAILS, userId);
        createBuiltInField("assignee", "Assignee", FieldDefinition.FieldType.USER,
                FieldDefinition.FieldRenderer.USER_PICKER, FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE, userId);
        createBuiltInField("reporter", "Reporter", FieldDefinition.FieldType.USER,
                FieldDefinition.FieldRenderer.USER_PICKER, FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE, userId);
        createBuiltInField("creator", "Creator", FieldDefinition.FieldType.USER,
                FieldDefinition.FieldRenderer.USER_PICKER, FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE, userId);
        createBuiltInField("labels", "Labels", FieldDefinition.FieldType.LABEL,
                FieldDefinition.FieldRenderer.LABEL_EDITOR, FieldDefinition.ScreenRegion.SIDEBAR_DETAILS, userId);
        createBuiltInField("components", "Components", FieldDefinition.FieldType.COMPONENT,
                FieldDefinition.FieldRenderer.MULTI_SELECT, FieldDefinition.ScreenRegion.SIDEBAR_DETAILS, userId);
        createBuiltInField("affects_versions", "Affects Version/s", FieldDefinition.FieldType.VERSION,
                FieldDefinition.FieldRenderer.MULTI_SELECT, FieldDefinition.ScreenRegion.SIDEBAR_VERSIONS, userId);
        createBuiltInField("fix_versions", "Fix Version/s", FieldDefinition.FieldType.VERSION,
                FieldDefinition.FieldRenderer.MULTI_SELECT, FieldDefinition.ScreenRegion.SIDEBAR_VERSIONS, userId);
        createBuiltInField("security_level", "Security Level", FieldDefinition.FieldType.SECURITY_LEVEL,
                FieldDefinition.FieldRenderer.SECURITY_LEVEL, FieldDefinition.ScreenRegion.SIDEBAR_DETAILS, userId);
        createBuiltInField("due_date", "Due Date", FieldDefinition.FieldType.DATE,
                FieldDefinition.FieldRenderer.DATETIME_PICKER, FieldDefinition.ScreenRegion.SIDEBAR_DATES, userId);
        createBuiltInField("created", "Created", FieldDefinition.FieldType.DATETIME,
                FieldDefinition.FieldRenderer.READ_ONLY, FieldDefinition.ScreenRegion.SIDEBAR_DATES, userId);
        createBuiltInField("updated", "Updated", FieldDefinition.FieldType.DATETIME,
                FieldDefinition.FieldRenderer.READ_ONLY, FieldDefinition.ScreenRegion.SIDEBAR_DATES, userId);
        createBuiltInField("resolution_date", "Resolved", FieldDefinition.FieldType.DATETIME,
                FieldDefinition.FieldRenderer.READ_ONLY, FieldDefinition.ScreenRegion.SIDEBAR_DATES, userId);
        createBuiltInField("original_estimate", "Original Estimate", FieldDefinition.FieldType.DURATION,
                FieldDefinition.FieldRenderer.DURATION, FieldDefinition.ScreenRegion.SIDEBAR_TIME, userId);
        createBuiltInField("remaining_estimate", "Remaining Estimate", FieldDefinition.FieldType.DURATION,
                FieldDefinition.FieldRenderer.DURATION, FieldDefinition.ScreenRegion.SIDEBAR_TIME, userId);
        createBuiltInField("time_spent", "Time Spent", FieldDefinition.FieldType.DURATION,
                FieldDefinition.FieldRenderer.DURATION, FieldDefinition.ScreenRegion.SIDEBAR_TIME, userId);
        createBuiltInField("votes", "Votes", FieldDefinition.FieldType.VOTES,
                FieldDefinition.FieldRenderer.VOTES, FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE, userId);
        createBuiltInField("watchers", "Watchers", FieldDefinition.FieldType.WATCHERS,
                FieldDefinition.FieldRenderer.WATCHERS, FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE, userId);
        createBuiltInField("epic_link", "Epic Link", FieldDefinition.FieldType.EPIC,
                FieldDefinition.FieldRenderer.EPIC_LINK, FieldDefinition.ScreenRegion.SIDEBAR_AGILE, userId);
        createBuiltInField("epic_name", "Epic Name", FieldDefinition.FieldType.TEXT,
                FieldDefinition.FieldRenderer.TEXT, FieldDefinition.ScreenRegion.SIDEBAR_AGILE, userId);
        createBuiltInField("epic_color", "Epic Colour", FieldDefinition.FieldType.TEXT,
                FieldDefinition.FieldRenderer.TEXT, FieldDefinition.ScreenRegion.SIDEBAR_AGILE, userId);
        createBuiltInField("story_points", "Story Points", FieldDefinition.FieldType.NUMBER,
                FieldDefinition.FieldRenderer.NUMBER, FieldDefinition.ScreenRegion.SIDEBAR_AGILE, userId);
        createBuiltInField("sprint", "Sprint", FieldDefinition.FieldType.SPRINT,
                FieldDefinition.FieldRenderer.SPRINT_SELECTOR, FieldDefinition.ScreenRegion.SIDEBAR_AGILE, userId);
        createBuiltInField("rank", "Rank", FieldDefinition.FieldType.TEXT,
                FieldDefinition.FieldRenderer.TEXT, FieldDefinition.ScreenRegion.SIDEBAR_AGILE, userId);
    }

    private void createBuiltInField(String key, String displayName, FieldDefinition.FieldType type,
                                   FieldDefinition.FieldRenderer renderer,
                                   FieldDefinition.ScreenRegion region, UUID userId) {
        if (!fieldDefinitionRepository.existsByFieldKey(key)) {
            FieldDefinition fieldDef = FieldDefinition.builder()
                    .fieldKey(key)
                    .displayName(displayName)
                    .fieldType(type)
                    .renderer(renderer)
                    .screenRegion(region)
                    .builtIn(true)
                    .custom(false)
                    .searchable(true)
                    .sortable(true)
                    .filterable(true)
                    .required(key.equals("summary") || key.equals("issue_type") || key.equals("status"))
                    .createdBy(userId)
                    .build();

            fieldDefinitionRepository.save(fieldDef);
            log.debug("Created built-in field: {}", key);
        }
    }

    @Transactional
    public void addFieldOption(UUID fieldDefId, String value, String label, UUID userId) {
        FieldDefinition fieldDef = fieldDefinitionRepository.findById(fieldDefId)
                .orElseThrow(() -> new IllegalArgumentException("Field definition not found: " + fieldDefId));

        if (fieldDef.getOptions() == null) {
            fieldDef.setOptions(new ArrayList<>());
        }

        FieldDefinition.FieldOption option = FieldDefinition.FieldOption.builder()
                .value(value)
                .label(label)
                .order(fieldDef.getOptions().size())
                .disabled(false)
                .build();

        fieldDef.getOptions().add(option);
        fieldDefinitionRepository.save(fieldDef);
    }

    @Transactional
    public void updateFieldOrder(List<UUID> orderedFieldIds) {
        for (int i = 0; i < orderedFieldIds.size(); i++) {
            fieldDefinitionRepository.findById(orderedFieldIds.get(i)).ifPresent(fieldDef -> {
                fieldDef.setVersion(fieldDef.getVersion() + 1);
                fieldDefinitionRepository.save(fieldDef);
            });
        }
    }
}