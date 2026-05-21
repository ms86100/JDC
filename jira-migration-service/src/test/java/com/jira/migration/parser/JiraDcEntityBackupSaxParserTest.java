package com.jira.migration.parser;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JiraDcEntityBackupSaxParserTest {

    @Test
    void streamsEntityBackupWithoutDom() throws Exception {
        String xml = loadEntitySample();
        Path temp = Files.createTempFile("entity-backup", ".xml");
        try {
            Files.writeString(temp, xml);
            List<JiraDcXmlParser.ParsedEntity> entities = JiraDcEntityBackupSaxParser.parse(temp);
            assertEquals(4, entities.size());
            Map<String, Long> byType = entities.stream()
                    .collect(Collectors.groupingBy(JiraDcXmlParser.ParsedEntity::getEntityType, Collectors.counting()));
            assertTrue(byType.containsKey("Issue"));
            assertTrue(byType.containsKey("Comment"));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String loadEntitySample() throws Exception {
        try (InputStream in = JiraDcEntityBackupSaxParserTest.class.getResourceAsStream(
                "/samples/jira-dc-minimal-comment-attachment.xml")) {
            assertNotNull(in, "entity sample missing");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
