package com.jira.migration.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Applies default values for mandatory target fields when source cells are empty (P3-08).
 */
@Service
public class FieldDefaultValueService {

    public List<Map<String, String>> applyDefaults(
            List<Map<String, String>> rows,
            Map<String, Object> fieldDefaults) {

        if (fieldDefaults == null || fieldDefaults.isEmpty()) {
            return rows;
        }

        List<Map<String, String>> result = new ArrayList<>(rows.size());
        for (Map<String, String> row : rows) {
            Map<String, String> copy = new LinkedHashMap<>(row);
            fieldDefaults.forEach((targetField, defaultVal) -> {
                if (defaultVal == null) {
                    return;
                }
                String key = targetField.toLowerCase(Locale.ROOT);
                String existing = copy.get(key);
                if (existing == null || existing.isBlank()) {
                    copy.put(key, defaultVal.toString());
                }
            });
            result.add(copy);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseDefaults(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> out = new HashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return Map.of();
    }
}
