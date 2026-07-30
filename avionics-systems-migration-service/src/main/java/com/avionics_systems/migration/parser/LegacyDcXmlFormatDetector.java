package com.avionics_systems.migration.parser;

/**
 * Detects which Legacy DC XML export format is in use before parsing.
 */
public final class LegacyDcXmlFormatDetector {

    private LegacyDcXmlFormatDetector() {
    }

    public static LegacyDcXmlFormat detect(String xmlContent) {
        if (xmlContent == null || xmlContent.isBlank()) {
            return LegacyDcXmlFormat.UNKNOWN;
        }
        String trimmed = xmlContent.stripLeading();
        if (trimmed.startsWith("<?xml")) {
            int endDecl = trimmed.indexOf("?>");
            if (endDecl >= 0) {
                trimmed = trimmed.substring(endDecl + 2).stripLeading();
            }
        }
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("<rss")) {
            return LegacyDcXmlFormat.RSS_092;
        }
        if (lower.contains("<legacydcbackup") || lower.contains("<entity>")
                && lower.contains("<entityname>")) {
            return LegacyDcXmlFormat.ENTITY_BACKUP;
        }
        if (lower.contains("<entity-engine-xml") || lower.contains("entities.xml")
                || (lower.contains("<entity") && lower.contains("issue"))) {
            return LegacyDcXmlFormat.ENTITIES_XML;
        }
        if (lower.contains("<entity>") && lower.contains("<entityname>")) {
            return LegacyDcXmlFormat.ENTITY_BACKUP;
        }
        return LegacyDcXmlFormat.UNKNOWN;
    }
}
