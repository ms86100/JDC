package com.jira.migration.parser;

/**
 * Detected Jira Data Center XML export shapes.
 */
public enum JiraDcXmlFormat {
    /** RSS 0.92 {@code <rss><channel><item>...} issue export (e.g. jira_dc_issue_export.xml). */
    RSS_092,
    /** {@code <JiraDcBackup><Entity><entityName>...} migration wizard contract. */
    ENTITY_BACKUP,
    /** Native Atlassian backup {@code entities.xml} inside DC ZIP. */
    ENTITIES_XML,
    UNKNOWN
}
