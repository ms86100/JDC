package com.avionics_systems.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Applies UI/API field mappings from source CSV columns to canonical issue field names.
 */
@Service
@Slf4j
public class CsvFieldMappingService {

    /** Canonical keys that map to core issue columns (not stored as custom field values). */
    private static final Set<String> STANDARD_ISSUE_FIELD_KEYS = Set.of(
            "summary", "description", "issue_type", "issue_type_id", "priority", "status", "resolution",
            "project_key", "project", "project_id", "assignee", "reporter", "creator", "labels", "components",
            "fix_version", "fix_versions", "affects_version", "affects_versions", "due_date", "parent_key",
            "parent", "epic_link", "epic", "sprint", "environment", "security_level", "issue_key", "issuekey",
            "issue_id", "created", "updated", "resolved", "votes", "watches", "time_spent", "remaining_estimate",
            "original_estimate", "comment", "comment_body", "comment_author", "author", "entity_type",
            "attachment_path", "attachment_url", "attachments", "attachment", "file_name", "filename",
            "row_number", "parentissuekey", "parent_issue_key", "issuetype", "projectkey", "projectname"
    );

    private static final Map<String, String> TARGET_ALIASES = Map.ofEntries(
            Map.entry("summary", "summary"),
            Map.entry("description", "description"),
            Map.entry("issuetype", "issue_type"),
            Map.entry("issue type", "issue_type"),
            Map.entry("issue_type", "issue_type"),
            Map.entry("priority", "priority"),
            Map.entry("status", "status"),
            Map.entry("project", "project_key"),
            Map.entry("project_key", "project_key"),
            Map.entry("assignee", "assignee"),
            Map.entry("reporter", "reporter"),
            Map.entry("labels", "labels"),
            Map.entry("components", "components"),
            Map.entry("fixversion", "fix_version"),
            Map.entry("affectedversion", "affects_version"),
            Map.entry("duedate", "due_date"),
            Map.entry("parent", "parent_key"),
            Map.entry("epic", "epic_link"),
            Map.entry("sprint", "sprint"),
            Map.entry("environment", "environment"),
            Map.entry("securitylevel", "security_level")
    );

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> applyMappings(
            List<Map<String, String>> rows,
            Object fieldMappingsOption) {

        if (fieldMappingsOption == null) {
            return rows;
        }

        List<Map<String, Object>> mappings = parseMappings(fieldMappingsOption);
        if (mappings.isEmpty()) {
            return rows;
        }

        List<Map<String, String>> result = new ArrayList<>(rows.size());
        for (Map<String, String> row : rows) {
            result.add(applyRowMapping(row, mappings));
        }
        return result;
    }

    private Map<String, String> applyRowMapping(Map<String, String> row, List<Map<String, Object>> mappings) {
        Map<String, String> mapped = new LinkedHashMap<>();
        // Preserve original keys for traceability
        row.forEach((k, v) -> mapped.put(k.toLowerCase(Locale.ROOT), v));

        for (Map<String, Object> mapping : mappings) {
            if (!Boolean.TRUE.equals(mapping.get("mapped"))) {
                continue;
            }
            String sourceColumn = stringVal(mapping.get("sourceColumn"));
            if (sourceColumn == null) {
                sourceColumn = stringVal(mapping.get("sourceField"));
            }
            String targetField = stringVal(mapping.get("targetField"));
            if (sourceColumn == null || targetField == null) {
                continue;
            }

            String sourceKey = sourceColumn.toLowerCase(Locale.ROOT).trim().replace(" ", "_");
            String value = row.get(sourceKey);
            if (value == null) {
                value = row.get(sourceColumn.toLowerCase(Locale.ROOT).trim());
            }
            if (value == null) {
                value = row.get(sourceColumn);
            }
            if (value == null || value.isBlank()) {
                continue;
            }

            String canonical = TARGET_ALIASES.getOrDefault(
                    targetField.toLowerCase(Locale.ROOT).trim(),
                    targetField.toLowerCase(Locale.ROOT).trim());
            mapped.put(canonical, value);
        }
        return mapped;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseMappings(Object fieldMappingsOption) {
        if (fieldMappingsOption instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    out.add((Map<String, Object>) m);
                }
            }
            return out;
        }
        return List.of();
    }

    private String stringVal(Object o) {
        return o == null ? null : o.toString().trim();
    }

    /**
     * Splits a mapped CSV row into core issue fields and custom field values (Legacy DC import behavior).
     */
    public Map<String, Object> buildIssueDataFromCsvRow(
            Map<String, String> rowData,
            String issueKey,
            int rowNum,
            UUID targetProjectId) {

        Map<String, Object> issueData = new LinkedHashMap<>();
        Map<String, Object> customFields = new LinkedHashMap<>();
        List<Map<String, String>> pendingAttachmentRefs = new ArrayList<>();

        for (Map.Entry<String, String> entry : rowData.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT).trim();
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            if ("attachments".equals(key) || "attachment".equals(key)) {
                for (String part : value.split("[;|]")) {
                    String ref = part.trim();
                    if (!ref.isBlank()) {
                        Map<String, String> refEntry = new LinkedHashMap<>();
                        refEntry.put("reference", ref);
                        int slash = Math.max(ref.lastIndexOf('/'), ref.lastIndexOf('\\'));
                        refEntry.put("fileName", slash >= 0 ? ref.substring(slash + 1) : ref);
                        pendingAttachmentRefs.add(refEntry);
                    }
                }
            } else if (STANDARD_ISSUE_FIELD_KEYS.contains(key) || isCustomFieldKey(key)) {
                if (isCustomFieldKey(key)) {
                    String label = extractCustomFieldLabel(key);
                    customFields.put(normalizeCustomKey(label), value);
                } else {
                    issueData.put(key, value);
                }
            } else {
                customFields.put(key, value);
            }
        }

        if (!customFields.isEmpty()) {
            issueData.put("customFields", customFields);
        }
        if (!pendingAttachmentRefs.isEmpty()) {
            issueData.put("_pendingAttachmentRefs", pendingAttachmentRefs);
        }
        issueData.put("issueKey", issueKey);
        issueData.put("rowNumber", rowNum);
        String parentKey = firstNonBlank(rowData, "parent_key", "parentissuekey", "parent_issue_key", "parent");
        if (parentKey != null) {
            issueData.put("parentIssueKey", parentKey);
        }
        if (rowData.containsKey("epic_link")) {
            issueData.put("epicLink", rowData.get("epic_link"));
        }
        if (targetProjectId != null) {
            issueData.put("projectId", targetProjectId.toString());
        }
        return issueData;
    }

    private static boolean isCustomFieldKey(String key) {
        return key.contains("custom field (") || key.contains("custom_field_(");
    }

    private String extractCustomFieldLabel(String header) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)custom[\\s_]+field[\\s_]*\\(?([^)]+)\\)")
                .matcher(header);
        return m.find() ? m.group(1).trim() : header;
    }

    private String normalizeCustomKey(String label) {
        return label.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    private static String firstNonBlank(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String v = row.get(key);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
