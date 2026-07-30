package com.avionics_systems.migration.parser;

/**
 * Detected Legacy Data Center XML export shapes.
 */
public enum LegacyDcXmlFormat {
    /** RSS 0.92 {@code <rss><channel><item>...} issue export (e.g. legacy_dc_issue_export.xml). */
    RSS_092,
    /** {@code <LegacyDcBackup><Entity><entityName>...} migration wizard contract. */
    ENTITY_BACKUP,
    /** Native Atlassian backup {@code entities.xml} inside DC ZIP. */
    ENTITIES_XML,
    UNKNOWN
}
