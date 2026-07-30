package com.avionics_systems.migration.service.field;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Validates source→target field type compatibility for migration mappings (P3-03).
 */
@Component
public class FieldTypeCompatibilityValidator {

    private static final Set<String> TEXT_TYPES = Set.of("TEXT", "TEXTAREA", "RICHTEXT", "STRING", "URL", "EMAIL");
    private static final Set<String> SELECT_TYPES = Set.of("SINGLE_SELECT", "RADIO", "ENUM", "STATUS", "PRIORITY", "ISSUE_TYPE");
    private static final Set<String> MULTI_TYPES = Set.of("MULTI_SELECT", "LABEL", "ARRAY", "CHECKBOX");

    public boolean isCompatible(String sourceDataType, String targetFieldType) {
        if (sourceDataType == null || targetFieldType == null) {
            return true;
        }
        String src = sourceDataType.toUpperCase(Locale.ROOT);
        String tgt = targetFieldType.toUpperCase(Locale.ROOT);

        // STRING/TEXT sources are always compatible with select and multi-select targets
        // because CSV values like "Bug", "High", "Open" are resolved by name during import.
        if (TEXT_TYPES.contains(src) && (SELECT_TYPES.contains(tgt) || MULTI_TYPES.contains(tgt))) {
            return true;
        }
        if (SELECT_TYPES.contains(src) && MULTI_TYPES.contains(tgt) && !tgt.equals("ARRAY")) {
            return false;
        }
        if (MULTI_TYPES.contains(src) && SELECT_TYPES.contains(tgt)) {
            return false;
        }
        if ("NUMBER".equals(src) && (TEXT_TYPES.contains(tgt) || SELECT_TYPES.contains(tgt))) {
            return false;
        }
        if (("USER".equals(src) || "GROUP".equals(src)) && TEXT_TYPES.contains(tgt)) {
            return false;
        }
        if (("DATE".equals(src) || "DATETIME".equals(src)) && !Set.of("DATE", "DATETIME", "TIME", "STRING", "TEXT").contains(tgt)) {
            return false;
        }
        return true;
    }

    public String incompatibilityReason(String sourceDataType, String targetFieldType) {
        if (isCompatible(sourceDataType, targetFieldType)) {
            return null;
        }
        return String.format("Cannot map %s source to %s target field", sourceDataType, targetFieldType);
    }
}
