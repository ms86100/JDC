package com.jira.migration.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JiraDcXmlParserTest {

    @Test
    void parsesWrappedBackupWithCommentAndAttachment() throws Exception {
        String xml = loadSampleXml();
        JiraDcXmlParser parser = new JiraDcXmlParser(
                null, null, null, null, new ObjectMapper());

        JiraDcXmlParser.ParseResult result = parser.parseXmlBackup(xml, UUID.randomUUID());

        assertEquals(4, result.getTotalEntities());
        Map<String, Long> byType = result.getEntities().stream()
                .collect(Collectors.groupingBy(JiraDcXmlParser.ParsedEntity::getEntityType, Collectors.counting()));
        assertEquals(1L, byType.get("Project"));
        assertEquals(1L, byType.get("Issue"));
        assertEquals(1L, byType.get("Comment"));
        assertEquals(1L, byType.get("Attachment"));

        JiraDcXmlParser.ParsedEntity comment = find(result, "Comment");
        assertEquals("DEMO-1", comment.getFields().get("issue"));
        assertTrue(comment.getFields().get("body").contains("validation comment"));

        JiraDcXmlParser.ParsedEntity attachment = find(result, "Attachment");
        assertEquals("DEMO-1", attachment.getFields().get("issue"));
        assertEquals("dc-sample-readme.txt", attachment.getFields().get("filename"));
        byte[] decoded = Base64.getDecoder().decode(attachment.getFields().get("file"));
        assertEquals("Hello from Jira DC attachment test", new String(decoded, StandardCharsets.UTF_8));

        JiraDcEntityMapper.AttachmentPayload payload = JiraDcEntityMapper.toAttachmentPayload(
                attachment.getFields(), attachment.getEntityKey(), Map.of("DEMO-1", UUID.randomUUID().toString()));
        assertEquals(decoded.length, payload.content().length);
    }

    @Test
    void issueEntityKeyUsesProjectAndId() throws Exception {
        String xml = loadSampleXml();
        JiraDcXmlParser parser = new JiraDcXmlParser(null, null, null, null, new ObjectMapper());
        JiraDcXmlParser.ParseResult result = parser.parseXmlBackup(xml, UUID.randomUUID());
        assertEquals(JiraDcXmlFormat.ENTITY_BACKUP, result.getXmlFormat());
        JiraDcXmlParser.ParsedEntity issue = find(result, "Issue");
        assertEquals("DEMO-1", issue.getEntityKey());
    }

    private static JiraDcXmlParser.ParsedEntity find(JiraDcXmlParser.ParseResult result, String type) {
        return result.getEntities().stream()
                .filter(e -> type.equals(e.getEntityType()))
                .findFirst()
                .orElseThrow();
    }

    private static String loadSampleXml() throws Exception {
        try (InputStream in = JiraDcXmlParserTest.class.getResourceAsStream(
                "/samples/jira-dc-minimal-comment-attachment.xml")) {
            assertNotNull(in, "sample XML missing from test resources");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
