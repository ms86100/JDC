package com.jira.migration.parser;

/**
 * Detects which Jira DC XML export format is in use before parsing.
 */
public final class JiraDcXmlFormatDetector {

    private JiraDcXmlFormatDetector() {
    }

    public static JiraDcXmlFormat detect(String xmlContent) {
        if (xmlContent == null || xmlContent.isBlank()) {
            return JiraDcXmlFormat.UNKNOWN;
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
            return JiraDcXmlFormat.RSS_092;
        }
        if (lower.contains("<jiradcbackup") || lower.contains("<entity>")
                && lower.contains("<entityname>")) {
            return JiraDcXmlFormat.ENTITY_BACKUP;
        }
        if (lower.contains("<entity-engine-xml") || lower.contains("entities.xml")
                || (lower.contains("<entity") && lower.contains("issue"))) {
            return JiraDcXmlFormat.ENTITIES_XML;
        }
        if (lower.contains("<entity>") && lower.contains("<entityname>")) {
            return JiraDcXmlFormat.ENTITY_BACKUP;
        }
        return JiraDcXmlFormat.UNKNOWN;
    }
}
