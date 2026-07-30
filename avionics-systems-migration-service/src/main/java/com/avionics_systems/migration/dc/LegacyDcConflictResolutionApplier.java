package com.avionics_systems.migration.dc;

import com.avionics_systems.migration.parser.LegacyDcXmlParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies UI-provided conflict resolutions from job options before import.
 */
public final class LegacyDcConflictResolutionApplier {

    private LegacyDcConflictResolutionApplier() {
    }

    @SuppressWarnings("unchecked")
    public static List<LegacyDcXmlParser.ParsedEntity> applySkipEntities(
            List<LegacyDcXmlParser.ParsedEntity> entities,
            Map<String, Object> options) {
        if (options == null || entities == null || entities.isEmpty()) {
            return entities;
        }
        Object raw = options.get("conflictResolutions");
        if (!(raw instanceof List<?> resolutions) || resolutions.isEmpty()) {
            return entities;
        }
        Set<String> skipKeys = new HashSet<>();
        for (Object item : resolutions) {
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            if (!"SKIP_ENTITY".equals(String.valueOf(m.get("action")))) {
                continue;
            }
            Object key = m.get("entityKey");
            if (key != null && !String.valueOf(key).isBlank()) {
                skipKeys.add(String.valueOf(key));
            }
        }
        if (skipKeys.isEmpty()) {
            return entities;
        }
        return entities.stream()
                .filter(e -> !skipKeys.contains(e.getEntityKey()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public static void applyFieldOverrides(List<LegacyDcXmlParser.ParsedEntity> entities, Map<String, Object> options) {
        if (options == null || entities == null) {
            return;
        }
        Object raw = options.get("conflictResolutions");
        if (!(raw instanceof List<?> resolutions) || resolutions.isEmpty()) {
            return;
        }
        Map<String, Map<String, String>> overridesByEntity = new HashMap<>();
        for (Object item : resolutions) {
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            String action = m.get("action") != null ? String.valueOf(m.get("action")) : "";
            if (!"USE_DEFAULT".equals(action) && !"OVERRIDE_VALUE".equals(action)) {
                continue;
            }
            String entityKey = m.get("entityKey") != null ? String.valueOf(m.get("entityKey")) : null;
            String field = m.get("field") != null ? String.valueOf(m.get("field")) : null;
            if (entityKey == null || entityKey.isBlank() || field == null || field.isBlank()) {
                continue;
            }
            String value = m.get("overrideValue") != null ? String.valueOf(m.get("overrideValue")) : "";
            overridesByEntity
                    .computeIfAbsent(entityKey, k -> new HashMap<>())
                    .put(field, value);
        }
        if (overridesByEntity.isEmpty()) {
            return;
        }
        for (LegacyDcXmlParser.ParsedEntity entity : entities) {
            Map<String, String> overrides = overridesByEntity.get(entity.getEntityKey());
            if (overrides == null || overrides.isEmpty()) {
                continue;
            }
            Map<String, String> fields = entity.getFields();
            if (fields == null) {
                continue;
            }
            for (Map.Entry<String, String> ov : overrides.entrySet()) {
                fields.put(ov.getKey(), ov.getValue());
            }
        }
    }
}
