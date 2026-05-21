package com.jira.migration.parser;

import com.jira.migration.dc.JiraDcImportValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise DC fixtures — entities.xml + RSS — parity regression gate.
 */
class JiraDcEnterpriseFixtureTest {

    @TempDir
    Path tempDir;

    @Test
    void enterpriseEntitiesXml_parsesWorklogSubTaskWatcherPlugin() throws Exception {
        Path file = copyResource("/samples/issues/enterprise-dc-entities.xml", "enterprise-dc-entities.xml");
        var entities = JiraDcEntitiesXmlParser.parse(file);
        Map<String, Long> byType = entities.stream()
                .collect(Collectors.groupingBy(JiraDcXmlParser.ParsedEntity::getEntityType, Collectors.counting()));

        assertTrue(byType.getOrDefault("Issue", 0L) >= 1);
        assertTrue(byType.getOrDefault("SubTask", 0L) >= 1, () -> "Sub-task issue type: " + byType);
        assertTrue(byType.getOrDefault("Worklog", 0L) >= 1);
        assertTrue(byType.getOrDefault("Watcher", 0L) >= 1);
        assertTrue(byType.getOrDefault("Vote", 0L) >= 1);
        assertTrue(byType.getOrDefault("History", 0L) >= 2);
        assertTrue(byType.getOrDefault("PluginEntity", 0L) >= 1);
        assertTrue(byType.getOrDefault("Component", 0L) >= 1);
        assertTrue(byType.getOrDefault("Version", 0L) >= 1);
    }

    @Test
    void enterpriseRss_parsesChannelItems() throws Exception {
        String xml = loadResource("/samples/issues/enterprise-dc-export.xml");
        JiraDcXmlParser parser = new JiraDcXmlParser(null, null, null, null, null);
        var result = parser.parseXmlBackup(xml, UUID.randomUUID());
        assertEquals(JiraDcXmlFormat.RSS_092, result.getXmlFormat());
        assertTrue(result.getTotalEntities() >= 2);
    }

    @Test
    void enterpriseEntities_validatesWithoutBlockers() throws Exception {
        Path file = copyResource("/samples/issues/enterprise-dc-entities.xml", "entities.xml");
        var entities = JiraDcEntitiesXmlParser.parse(file);
        JiraDcImportValidationService validation = new JiraDcImportValidationService(null, null, null);
        var report = validation.validate(null, null, entities, null, false);
        assertTrue(report.valid(), () -> "blockers: " + report.blockerCount());
        assertEquals(0, report.blockerCount());
    }

    @Test
    void streamingSoak_parsesTenThousandIssuesUnderBudget() throws Exception {
        Path file = tempDir.resolve("soak-10k-entities.xml");
        StringBuilder xml = new StringBuilder();
        xml.append("<entity-engine-xml>");
        for (int i = 1; i <= 10_000; i++) {
            xml.append("<Issue id=\"").append(i)
                    .append("\" projectKey=\"SOC\" number=\"").append(i)
                    .append("\" summary=\"Soak issue ").append(i)
                    .append("\" status=\"Open\"/>");
        }
        xml.append("</entity-engine-xml>");
        Files.writeString(file, xml.toString());

        long start = System.currentTimeMillis();
        var entities = JiraDcEntitiesXmlParser.parse(file);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(10_000, entities.stream().filter(e -> "Issue".equals(e.getEntityType())).count());
        assertTrue(elapsed < 120_000, "Parse 10k issues in <120s, took " + elapsed + "ms");
    }

    @Test
    void streamingSoak_parsesTwoThousandIssuesUnderBudget() throws Exception {
        Path file = tempDir.resolve("soak-entities.xml");
        StringBuilder xml = new StringBuilder();
        xml.append("<entity-engine-xml>");
        for (int i = 1; i <= 2000; i++) {
            xml.append("<Issue id=\"").append(i)
                    .append("\" projectKey=\"SOC\" number=\"").append(i)
                    .append("\" summary=\"Soak issue ").append(i)
                    .append("\" status=\"Open\"/>");
        }
        xml.append("</entity-engine-xml>");
        Files.writeString(file, xml.toString());

        long start = System.currentTimeMillis();
        var entities = JiraDcEntitiesXmlParser.parse(file);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(2000, entities.stream().filter(e -> "Issue".equals(e.getEntityType())).count());
        assertTrue(elapsed < 30_000, "Parse 2000 issues in <30s, took " + elapsed + "ms");
    }

    private Path copyResource(String classpath, String name) throws Exception {
        Path dest = tempDir.resolve(name);
        Files.writeString(dest, loadResource(classpath));
        return dest;
    }

    private static String loadResource(String path) throws Exception {
        try (InputStream in = JiraDcEnterpriseFixtureTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "Missing fixture: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
