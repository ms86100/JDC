package com.jira.migration.dc;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Resolves Jira DC custom field values to platform payloads (types, multi-value, agile fields).
 */
@Component
public class JiraDcCustomFieldResolver {

    private static final Map<String, String> PLUGIN_ALIASES = Map.ofEntries(
            Map.entry("com.pyxis.greenhopper.jira:gh-epic-link", "epicLink"),
            Map.entry("com.pyxis.greenhopper.jira:gh-sprint", "sprint"),
            Map.entry("Story Points", "story_points")
    );

    public Map<String, Object> resolve(Map<String, String> issueFields) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        if (issueFields == null) {
            return resolved;
        }

        for (Map.Entry<String, String> e : issueFields.entrySet()) {
            String key = e.getKey();
            if (key == null || !key.startsWith("customfield_") || key.endsWith("_name")) {
                continue;
            }
            String raw = e.getValue();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            if (raw.contains(",")) {
                resolved.put(key, Arrays.asList(raw.split(",")));
            } else {
                resolved.put(key, raw.trim());
            }
            String nameKey = key + "_name";
            if (issueFields.containsKey(nameKey)) {
                String cfName = issueFields.get(nameKey);
                resolved.put(key + ".name", cfName);
                String alias = PLUGIN_ALIASES.get(cfName);
                if (alias != null) {
                    resolved.put(alias, raw.trim());
                }
            }
        }

        String epic = first(issueFields, "epicLink", "customfield_10014");
        if (epic != null) {
            resolved.put("epicLink", epic);
        }
        String sprint = first(issueFields, "sprint", "customfield_10016");
        if (sprint != null) {
            resolved.put("sprint", sprint);
        }

        return resolved;
    }

    public String detectType(String value) {
        if (value == null) {
            return "unknown";
        }
        if (value.contains(",")) {
            return "multi-select";
        }
        if (value.contains("/")) {
            return "cascading-select";
        }
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            return "number";
        }
        if (value.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            return "date";
        }
        return "text";
    }

    public Map<String, Object> fallbackRaw(Map<String, String> issueFields) {
        Map<String, Object> raw = new HashMap<>();
        for (Map.Entry<String, String> e : issueFields.entrySet()) {
            if (e.getKey() != null && e.getKey().startsWith("customfield_")) {
                raw.put(e.getKey(), e.getValue());
            }
        }
        return raw;
    }

    private static String first(Map<String, String> f, String... keys) {
        for (String k : keys) {
            if (f.containsKey(k) && f.get(k) != null && !f.get(k).isBlank()) {
                return f.get(k);
            }
        }
        return null;
    }
}
