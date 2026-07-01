package com.jira.migration.dc;

import com.jira.migration.parser.JiraDcXmlFormat;
import com.jira.migration.parser.JiraDcXmlFormatDetector;
import com.jira.migration.parser.JiraDcXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class JiraDcImportEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void attachmentBundleResolvesFromDirectory() throws IOException {
        Path bundle = tempDir.resolve("attachments");
        Files.createDirectories(bundle);
        Files.writeString(bundle.resolve("30001"), "zip-content", StandardCharsets.UTF_8);

        JiraDcAttachmentBundleResolver resolver = new JiraDcAttachmentBundleResolver();
        var resolved = resolver.resolve(bundle, "30001", "payment-error-log.zip");
        assertTrue(resolved.hasContent());
        assertEquals("zip-content", new String(resolved.content(), StandardCharsets.UTF_8));
    }

    @Test
    void attachmentBundleRejectsPathTraversal() {
        assertEquals("etc/passwd", JiraDcAttachmentBundleResolver.sanitizeFileName("../etc/passwd"));
    }

    @Test
    void validationDetectsDuplicateIssueKeys() {
        JiraDcImportValidationService validationService =
                new JiraDcImportValidationService(null, null, null);
        JiraDcXmlParser.ParsedEntity e1 = issue("ENT-1");
        JiraDcXmlParser.ParsedEntity e2 = issue("ENT-1");
        var report = validationService.validate(null, null, java.util.List.of(e1, e2), null, false);
        assertFalse(report.valid());
        assertTrue(report.blockerCount() >= 1);
    }

    @Test
    void entitiesXmlFormatDetected() {
        String xml = "<entity-engine-xml><Issue id=\"1\" projectKey=\"ENT\" number=\"99\"/></entity-engine-xml>";
        assertEquals(JiraDcXmlFormat.ENTITIES_XML, JiraDcXmlFormatDetector.detect(xml));
    }

    @Test
    void customFieldResolverMapsStoryPoints() {
        JiraDcCustomFieldResolver resolver = new JiraDcCustomFieldResolver();
        java.util.Map<String, String> fields = new java.util.HashMap<>();
        fields.put("customfield_10010", "13");
        fields.put("customfield_10010_name", "Story Points");
        var resolved = resolver.resolve(fields);
        assertEquals("13", resolved.get("story_points"));
    }

    @Test
    void attachmentBundleFromZip() throws IOException {
        Path zip = tempDir.resolve("bundle.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("data/attachments/30001"));
            zos.write("from-zip".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        var resolved = new JiraDcAttachmentBundleResolver().resolve(zip, "30001", null);
        assertTrue(resolved.hasContent());
    }

    private static JiraDcXmlParser.ParsedEntity issue(String key) {
        JiraDcXmlParser.ParsedEntity e = new JiraDcXmlParser.ParsedEntity();
        e.setEntityType("Issue");
        e.setEntityKey(key);
        java.util.Map<String, String> f = new java.util.HashMap<>();
        f.put("issueKey", key);
        f.put("project", "ENT");
        f.put("summary", "Test");
        f.put("status", "Open");
        f.put("issueType", "Task");
        e.setFields(f);
        return e;
    }
}
