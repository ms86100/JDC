package com.jira.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Applies UI/API field mappings from source CSV columns to canonical issue field names.
 */
@Service
@Slf4j
public class CsvFieldMappingService {

    private static final Map<String, String> TARGET_ALIASES = Map.ofEntries(
            Map.entry("summary", "summary"),
            Map.entry("description", "description"),
            Map.entry("issuetype", "issue_type"),
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

            String sourceKey = sourceColumn.toLowerCase(Locale.ROOT).trim();
            String value = row.get(sourceKey);
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
}
