package com.jira.migration.service.field;

import com.jira.migration.entity.field.FieldDefinition;
import com.jira.migration.entity.field.PluginFieldRegistry;
import com.jira.migration.repository.field.FieldDefinitionRepository;
import com.jira.migration.repository.field.PluginFieldRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for discovering fields from import payloads.
 * Scans and categorizes fields as standard, unknown, plugin, or agile.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldDiscoveryService {

    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final PluginFieldRegistryRepository pluginFieldRegistryRepository;

    private static final Set<String> STANDARD_FIELD_KEYS = Set.of(
            "summary", "description", "environment", "issuetype", "issue_type", "type",
            "status", "priority", "resolution", "assignee", "reporter", "creator",
            "created", "updated", "duedate", "due_date", "resolutiondate", "resolution_date",
            "labels", "components", "versions", "affects_versions", "fix_versions",
            "security", "security_level", "project", "parent", "epic", "epic_link",
            "epic_name", "epic_color", "story_points", "storypoints", "rank", "sprint",
            "original_estimate", "remaining_estimate", "time_spent", "votes", "watchers",
            "attachments", "linked_issues", "subtasks", "comments", "worklog"
    );

    private static final Set<String> AGILE_FIELD_PATTERNS = Set.of(
            "sprint", "story_points", "epic", "rank", "team", "velocity", "capacity"
    );

    private static final Map<String, String> AGILE_FIELD_ALIASES = Map.ofEntries(
            Map.entry("storypoints", "story_points"),
            Map.entry("story_points", "story_points"),
            Map.entry("storypoint", "story_points"),
            Map.entry("epiclink", "epic_link"),
            Map.entry("epic_link", "epic_link"),
            Map.entry("epiclink", "epic_link"),
            Map.entry("epicname", "epic_name"),
            Map.entry("epic_name", "epic_name"),
            Map.entry("epiccolor", "epic_color"),
            Map.entry("epic_color", "epic_color"),
            Map.entry("epicstatus", "epic_status"),
            Map.entry("parentlink", "parent_link"),
            Map.entry("parent_link", "parent_link")
    );

    private static final Map<String, String> PLUGIN_NAMESPACES = Map.ofEntries(
            Map.entry("customfield_", "custom"),
            Map.entry("cf_", "custom"),
            Map.entry("tempo_", "tempo"),
            Map.entry("xray_", "xray"),
            Map.entry("zephyr_", "zephyr"),
            Map.entry("structure_", "structure"),
            Map.entry("bigpicture_", "bigpicture"),
            Map.entry("jirasoftware_", "jira-software"),
            Map.entry("atlassian_", "atlassian")
    );

    private static final Set<String> JIRA_STANDARD_FIELDS = Set.of(
            "IssueKey", "Summary", "Description", "IssueType", "Status", "Priority",
            "Assignee", "Reporter", "Creator", "Created", "Updated", "DueDate",
            "Resolution", "Project", "ProjectKey", "ProjectName", "Labels", "Components",
            "AffectsVersions", "FixVersions", "SecurityLevel", "Parent", "Epic Link",
            "Epic Name", "Epic Color", "Story Points", "Sprint", "Rank", "Time Tracking",
            "Original Estimate", "Remaining Estimate", "Time Spent"
    );

    public record DiscoveredField(
            String sourceKey,
            String normalizedKey,
            FieldCategory category,
            FieldDefinition.FieldType suggestedType,
            FieldDefinition.ScreenRegion suggestedRegion,
            Map<String, Object> metadata,
            boolean isKnown,
            boolean requiresProvisioning
    ) {}

    public enum FieldCategory {
        STANDARD,
        AGILE,
        PLUGIN,
        MARKETPLACE,
        CUSTOM,
        UNKNOWN
    }

    public record FieldDiscoveryResult(
            List<DiscoveredField> discoveredFields,
            List<DiscoveredField> standardFields,
            List<DiscoveredField> agileFields,
            List<DiscoveredField> pluginFields,
            List<DiscoveredField> unknownFields,
            Map<String, List<String>> fieldGroupings,
            Set<String> missingFieldKeys
    ) {}

    public FieldDiscoveryResult discoverFields(Map<String, Object> payload) {
        log.info("Starting field discovery for payload with {} keys", payload.size());

        List<DiscoveredField> allDiscovered = new ArrayList<>();
        Map<String, List<String>> groupings = new HashMap<>();

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String sourceKey = entry.getKey();
            Object value = entry.getValue();
            DiscoveredField discovered = discoverField(sourceKey, value);
            allDiscovered.add(discovered);

            String group = categorizeFieldGroup(discovered.normalizedKey());
            groupings.computeIfAbsent(group, k -> new ArrayList<>()).add(sourceKey);
        }

        List<DiscoveredField> standard = allDiscovered.stream()
                .filter(f -> f.category() == FieldCategory.STANDARD)
                .toList();
        List<DiscoveredField> agile = allDiscovered.stream()
                .filter(f -> f.category() == FieldCategory.AGILE)
                .toList();
        List<DiscoveredField> plugin = allDiscovered.stream()
                .filter(f -> f.category() == FieldCategory.PLUGIN || f.category() == FieldCategory.MARKETPLACE)
                .toList();
        List<DiscoveredField> unknown = allDiscovered.stream()
                .filter(f -> f.category() == FieldCategory.UNKNOWN || f.category() == FieldCategory.CUSTOM)
                .toList();

        Set<String> missingFieldKeys = identifyMissingFields(allDiscovered);

        log.info("Field discovery complete: {} standard, {} agile, {} plugin, {} unknown fields",
                standard.size(), agile.size(), plugin.size(), unknown.size());

        return new FieldDiscoveryResult(
                allDiscovered, standard, agile, plugin, unknown, groupings, missingFieldKeys
        );
    }

    public FieldDiscoveryResult discoverFieldsFromList(List<Map<String, Object>> payloads) {
        log.info("Starting bulk field discovery for {} payloads", payloads.size());

        Map<String, Object> aggregatedMetadata = new HashMap<>();
        Set<String> allKeys = new HashSet<>();

        for (Map<String, Object> payload : payloads) {
            allKeys.addAll(payload.keySet());
        }

        for (String key : allKeys) {
            List<Object> values = payloads.stream()
                    .map(p -> p.get(key))
                    .filter(Objects::nonNull)
                    .toList();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sampleCount", values.size());
            metadata.put("nonNullCount", values.stream().filter(Objects::nonNull).count());
            metadata.put("sampleValues", values.stream().limit(5).toList());

            aggregatedMetadata.put(key, metadata);
        }

        Map<String, Object> combinedPayload = new HashMap<>();
        for (String key : allKeys) {
            combinedPayload.put(key, aggregatedMetadata.get(key));
        }

        return discoverFields(combinedPayload);
    }

    public DiscoveredField discoverField(String sourceKey, Object value) {
        String normalizedKey = normalizeFieldKey(sourceKey);
        boolean isKnown = fieldDefinitionRepository.existsByFieldKey(normalizedKey);

        FieldCategory category = categorizeField(normalizedKey, sourceKey);
        FieldDefinition.FieldType suggestedType = inferFieldType(value);
        FieldDefinition.ScreenRegion suggestedRegion = inferScreenRegion(normalizedKey, category);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sourceKey", sourceKey);
        metadata.put("sampleValue", value);
        metadata.put("valueClass", value != null ? value.getClass().getSimpleName() : "null");

        boolean requiresProvisioning = !isKnown && (category == FieldCategory.UNKNOWN || category == FieldCategory.CUSTOM);

        return new DiscoveredField(
                sourceKey, normalizedKey, category, suggestedType, suggestedRegion, metadata, isKnown, requiresProvisioning
        );
    }

    private String normalizeFieldKey(String key) {
        if (key == null) return "";

        String normalized = key.trim().toLowerCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace(".", "_")
                .replace("[", "_")
                .replace("]", "")
                .replace("__", "_");

        return AGILE_FIELD_ALIASES.getOrDefault(normalized, normalized);
    }

    private FieldCategory categorizeField(String normalizedKey, String sourceKey) {
        if (STANDARD_FIELD_KEYS.contains(normalizedKey)) {
            return FieldCategory.STANDARD;
        }

        for (String pattern : AGILE_FIELD_PATTERNS) {
            if (normalizedKey.contains(pattern)) {
                return FieldCategory.AGILE;
            }
        }

        for (Map.Entry<String, String> entry : PLUGIN_NAMESPACES.entrySet()) {
            if (normalizedKey.startsWith(entry.getKey()) || sourceKey.contains(entry.getKey())) {
                if ("tempo".equals(entry.getValue()) || "xray".equals(entry.getValue()) ||
                        "zephyr".equals(entry.getValue()) || "structure".equals(entry.getValue()) ||
                        "bigpicture".equals(entry.getValue())) {
                    return FieldCategory.MARKETPLACE;
                }
                return FieldCategory.PLUGIN;
            }
        }

        if (normalizedKey.startsWith("customfield") || normalizedKey.startsWith("cf_")) {
            return FieldCategory.CUSTOM;
        }

        for (String jiraField : JIRA_STANDARD_FIELDS) {
            if (sourceKey.equalsIgnoreCase(jiraField) || sourceKey.equalsIgnoreCase(jiraField.replace(" ", ""))) {
                return FieldCategory.STANDARD;
            }
        }

        return FieldCategory.UNKNOWN;
    }

    private FieldDefinition.FieldType inferFieldType(Object value) {
        if (value == null) return FieldDefinition.FieldType.TEXT;

        if (value instanceof String) {
            if (((String) value).length() > 500) {
                return FieldDefinition.FieldType.TEXTAREA;
            }
            if (((String) value).contains("@") && ((String) value).contains(".")) {
                return FieldDefinition.FieldType.EMAIL;
            }
            if (((String) value).startsWith("http://") || ((String) value).startsWith("https://")) {
                return FieldDefinition.FieldType.URL;
            }
            if (isDateString((String) value)) {
                return FieldDefinition.FieldType.DATE;
            }
            return FieldDefinition.FieldType.TEXT;
        }

        if (value instanceof Number) {
            if (value instanceof Double || value instanceof Float) {
                return FieldDefinition.FieldType.NUMBER;
            }
            return FieldDefinition.FieldType.NUMBER;
        }

        if (value instanceof Boolean) {
            return FieldDefinition.FieldType.CHECKBOX;
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof String) {
                    return FieldDefinition.FieldType.MULTI_SELECT;
                }
                if (first instanceof Map) {
                    return FieldDefinition.FieldType.CUSTOM;
                }
            }
            return FieldDefinition.FieldType.MULTI_SELECT;
        }

        if (value instanceof Map) {
            return FieldDefinition.FieldType.CUSTOM;
        }

        if (value instanceof UUID) {
            return FieldDefinition.FieldType.SINGLE_SELECT;
        }

        return FieldDefinition.FieldType.UNKNOWN;
    }

    private boolean isDateString(String value) {
        if (value == null || value.isEmpty()) return false;

        // Comprehensive date/datetime pattern matching
        // ISO 8601 formats
        if (Pattern.compile("\\d{4}-\\d{2}-\\d{2}").matcher(value).matches()) return true;
        if (Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:?\\d{0,2}").matcher(value).matches()) return true;
        if (Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}").matcher(value).matches()) return true;
        if (Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}").matcher(value).matches()) return true;
        if (Pattern.compile("\\d{4}/\\d{2}/\\d{2}").matcher(value).matches()) return true;
        if (Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(value).matches()) return true;

        // Common datetime patterns
        if (Pattern.compile("\\d{2}-\\w{3}-\\d{2,4} \\d{1,2}:\\d{2}(:\\d{2})?").matcher(value).matches()) return true;
        if (Pattern.compile("\\w{3} \\d{1,2},? \\d{4}").matcher(value).matches()) return true;

        // Unix timestamp (numeric string 10 or 13 digits)
        if (value.matches("\\d{10}") || value.matches("\\d{13}")) {
            try {
                long ts = Long.parseLong(value);
                // Valid range: year 2000 to year 2100
                if (ts > 946684800L && ts < 4102444800L) return true;
            } catch (NumberFormatException e) {
                // Not a timestamp
            }
        }

        return false;
    }

    private FieldDefinition.ScreenRegion inferScreenRegion(String normalizedKey, FieldCategory category) {
        if (normalizedKey.contains("description") || normalizedKey.contains("environment")) {
            return FieldDefinition.ScreenRegion.LEFT_DESCRIPTION;
        }

        if (normalizedKey.contains("assignee") || normalizedKey.contains("reporter") ||
                normalizedKey.contains("creator") || normalizedKey.contains("votes") ||
                normalizedKey.contains("watchers")) {
            return FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE;
        }

        if (normalizedKey.contains("priority") || normalizedKey.contains("resolution") ||
                normalizedKey.contains("components") || normalizedKey.contains("labels") ||
                normalizedKey.contains("security")) {
            return FieldDefinition.ScreenRegion.SIDEBAR_DETAILS;
        }

        if (normalizedKey.contains("estimate") || normalizedKey.contains("time_spent") ||
                normalizedKey.contains("worklog")) {
            return FieldDefinition.ScreenRegion.SIDEBAR_TIME;
        }

        if (normalizedKey.contains("sprint") || normalizedKey.contains("epic") ||
                normalizedKey.contains("story_points") || normalizedKey.contains("rank") ||
                normalizedKey.contains("team")) {
            return FieldDefinition.ScreenRegion.SIDEBAR_AGILE;
        }

        if (normalizedKey.contains("due_date") || normalizedKey.contains("created") ||
                normalizedKey.contains("updated") || normalizedKey.contains("resolved")) {
            return FieldDefinition.ScreenRegion.SIDEBAR_DATES;
        }

        if (normalizedKey.contains("version")) {
            return FieldDefinition.ScreenRegion.SIDEBAR_VERSIONS;
        }

        if (category == FieldCategory.AGILE) {
            return FieldDefinition.ScreenRegion.SIDEBAR_AGILE;
        }

        if (category == FieldCategory.PLUGIN || category == FieldCategory.MARKETPLACE) {
            return FieldDefinition.ScreenRegion.SIDEBAR_DETAILS;
        }

        return FieldDefinition.ScreenRegion.SIDEBAR;
    }

    private String categorizeFieldGroup(String normalizedKey) {
        if (normalizedKey.contains("description") || normalizedKey.contains("environment")) {
            return "description";
        }
        if (normalizedKey.contains("assignee") || normalizedKey.contains("reporter") ||
                normalizedKey.contains("creator")) {
            return "people";
        }
        if (normalizedKey.contains("sprint") || normalizedKey.contains("epic") ||
                normalizedKey.contains("story_points")) {
            return "agile";
        }
        if (normalizedKey.contains("estimate") || normalizedKey.contains("time_spent")) {
            return "time_tracking";
        }
        if (normalizedKey.contains("version")) {
            return "versions";
        }
        if (normalizedKey.contains("due_date") || normalizedKey.contains("created") ||
                normalizedKey.contains("updated")) {
            return "dates";
        }
        return "details";
    }

    private Set<String> identifyMissingFields(List<DiscoveredField> discoveredFields) {
        Set<String> missingKeys = new HashSet<>();

        for (DiscoveredField field : discoveredFields) {
            if (field.requiresProvisioning() && !field.isKnown()) {
                missingKeys.add(field.normalizedKey());
            }
        }

        return missingKeys;
    }

    public Optional<PluginFieldRegistry> findPluginFieldMapping(String fieldKey, String pluginKey) {
        return pluginFieldRegistryRepository.findByPluginKeyAndFieldKey(pluginKey, fieldKey);
    }

    public List<PluginFieldRegistry> findPluginFieldMappings(String pluginKey) {
        return pluginFieldRegistryRepository.findByPluginKey(pluginKey);
    }

    public boolean isAgileField(String fieldKey) {
        String normalized = normalizeFieldKey(fieldKey);
        for (String pattern : AGILE_FIELD_PATTERNS) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStandardField(String fieldKey) {
        return STANDARD_FIELD_KEYS.contains(normalizeFieldKey(fieldKey));
    }
}