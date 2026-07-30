package com.avionics_systems.migration.parser;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDcRss092ParserTest {

    @Test
    void detectsRssFormat() throws Exception {
        String xml = loadRssFixture();
        assertEquals(LegacyDcXmlFormat.RSS_092, LegacyDcXmlFormatDetector.detect(xml));
    }

    @Test
    void parsesCanonicalRssFixtureEndToEnd() throws Exception {
        String xml = loadRssFixture();
        LegacyDcXmlParser parser = new LegacyDcXmlParser(null, null, null, null, null);

        LegacyDcXmlParser.ParseResult result = parser.parseXmlBackup(xml, UUID.randomUUID());

        assertEquals(LegacyDcXmlFormat.RSS_092, result.getXmlFormat());
        assertEquals(5, result.getTotalEntities());

        Map<String, Long> byType = result.getEntities().stream()
                .collect(Collectors.groupingBy(LegacyDcXmlParser.ParsedEntity::getEntityType, Collectors.counting()));
        assertEquals(1L, byType.get("Project"));
        assertEquals(1L, byType.get("Issue"));
        assertEquals(1L, byType.get("Comment"));
        assertEquals(2L, byType.get("Attachment"));

        LegacyDcXmlParser.ParsedEntity issue = find(result, "Issue");
        assertEquals("ENT-1024", issue.getEntityKey());
        assertEquals("102455", issue.getFields().get("id"));
        assertEquals("ENT", issue.getFields().get("project"));
        assertEquals("13", issue.getFields().get("customfield_10010"));
        assertEquals("Story Points", issue.getFields().get("customfield_10010_name"));
        assertTrue(issue.getFields().get("summary").contains("payment processing"));

        Map<String, Object> issueData = LegacyDcEntityMapper.toIssueData(issue.getFields(), issue.getEntityKey());
        @SuppressWarnings("unchecked")
        Map<String, Object> cfs = (Map<String, Object>) issueData.get("customFields");
        assertNotNull(cfs);
        assertEquals("13", cfs.get("customfield_10010"));
        assertEquals("13", cfs.get("story_points"));

        LegacyDcXmlParser.ParsedEntity comment = find(result, "Comment");
        assertEquals("ENT-1024", comment.getFields().get("issue"));
        assertEquals("50001", comment.getFields().get("sourceCommentId"));
        assertTrue(comment.getFields().get("body").contains("retry orchestration"));

        List<LegacyDcXmlParser.ParsedEntity> attachments = result.getEntities().stream()
                .filter(e -> "Attachment".equals(e.getEntityType()))
                .toList();
        assertEquals(2, attachments.size());
        assertTrue(attachments.stream().anyMatch(a -> "payment-error-log.zip".equals(a.getFields().get("filename"))));
        assertTrue(attachments.stream().anyMatch(a -> "30001".equals(a.getFields().get("sourceAttachmentId"))));
    }

    @Test
    void parsesExtendedRssLabelsLinksWorklogChangelog() throws Exception {
        String xml = loadExtendedRssFixture();
        LegacyDcXmlParser parser = new LegacyDcXmlParser(null, null, null, null, null);
        LegacyDcXmlParser.ParseResult result = parser.parseXmlBackup(xml, UUID.randomUUID());

        assertEquals(LegacyDcXmlFormat.RSS_092, result.getXmlFormat());
        Map<String, Long> byType = result.getEntities().stream()
                .collect(Collectors.groupingBy(LegacyDcXmlParser.ParsedEntity::getEntityType, Collectors.counting()));
        assertEquals(2L, byType.get("Issue"));
        assertTrue(byType.getOrDefault("Label", 0L) >= 2L);
        assertTrue(byType.getOrDefault("IssueLink", 0L) >= 1L);
        assertTrue(byType.getOrDefault("Worklog", 0L) >= 1L);
        assertTrue(byType.getOrDefault("History", 0L) >= 1L);

        LegacyDcXmlParser.ParsedEntity link = result.getEntities().stream()
                .filter(e -> "IssueLink".equals(e.getEntityType()))
                .findFirst()
                .orElseThrow();
        assertEquals("Blocks", link.getFields().get("linkType"));
        assertEquals("EXT-1", link.getFields().get("sourceIssueKey"));
    }

    @Test
    void entityBackupFormatStillWorks() throws Exception {
        String xml = loadEntitySample();
        assertEquals(LegacyDcXmlFormat.ENTITY_BACKUP, LegacyDcXmlFormatDetector.detect(xml));

        LegacyDcXmlParser parser = new LegacyDcXmlParser(null, null, null, null, null);
        LegacyDcXmlParser.ParseResult result = parser.parseXmlBackup(xml, UUID.randomUUID());

        assertEquals(LegacyDcXmlFormat.ENTITY_BACKUP, result.getXmlFormat());
        assertEquals(4, result.getTotalEntities());
    }

    private static LegacyDcXmlParser.ParsedEntity find(LegacyDcXmlParser.ParseResult result, String type) {
        return result.getEntities().stream()
                .filter(e -> type.equals(e.getEntityType()))
                .findFirst()
                .orElseThrow();
    }

    private static String loadRssFixture() throws Exception {
        try (InputStream in = LegacyDcRss092ParserTest.class.getResourceAsStream("/samples/legacy_dc_issue_export.xml")) {
            assertNotNull(in, "RSS fixture missing");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String loadExtendedRssFixture() throws Exception {
        try (InputStream in = LegacyDcRss092ParserTest.class.getResourceAsStream("/samples/jira-dc-rss-extended.xml")) {
            assertNotNull(in, "extended RSS fixture missing");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String loadEntitySample() throws Exception {
        try (InputStream in = LegacyDcRss092ParserTest.class.getResourceAsStream(
                "/samples/jira-dc-minimal-comment-attachment.xml")) {
            assertNotNull(in, "entity sample missing");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
