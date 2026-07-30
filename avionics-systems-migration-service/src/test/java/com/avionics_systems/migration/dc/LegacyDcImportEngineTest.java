package com.avionics_systems.migration.dc;

import com.avionics_systems.migration.parser.LegacyDcXmlFormat;
import com.avionics_systems.migration.parser.LegacyDcXmlFormatDetector;
import com.avionics_systems.migration.parser.LegacyDcXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDcImportEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void attachmentBundleResolvesFromDirectory() throws IOException {
        Path bundle = tempDir.resolve("attachments");
        Files.createDirectories(bundle);
        Files.writeString(bundle.resolve("30001"), "zip-content", StandardCharsets.UTF_8);

        LegacyDcAttachmentBundleResolver resolver = new LegacyDcAttachmentBundleResolver();
        var resolved = resolver.resolve(bundle, "30001", "payment-error-log.zip");
        assertTrue(resolved.hasContent());
        assertEquals("zip-content", new String(resolved.content(), StandardCharsets.UTF_8));
    }

    @Test
    void attachmentBundleRejectsPathTraversal() {
        assertEquals("etc/passwd", LegacyDcAttachmentBundleResolver.sanitizeFileName("../etc/passwd"));
    }

    @Test
    void validationDetectsDuplicateIssueKeys() {
        LegacyDcImportValidationService validationService =
                new LegacyDcImportValidationService(null, null, null);
        LegacyDcXmlParser.ParsedEntity e1 = issue("ENT-1");
        LegacyDcXmlParser.ParsedEntity e2 = issue("ENT-1");
        var report = validationService.validate(null, null, java.util.List.of(e1, e2), null, false);
        assertFalse(report.valid());
        assertTrue(report.blockerCount() >= 1);
    }

    @Test
    void entitiesXmlFormatDetected() {
        String xml = "<entity-engine-xml><Issue id=\"1\" projectKey=\"ENT\" number=\"99\"/></entity-engine-xml>";
        assertEquals(LegacyDcXmlFormat.ENTITIES_XML, LegacyDcXmlFormatDetector.detect(xml));
    }

    @Test
    void customFieldResolverMapsStoryPoints() {
        LegacyDcCustomFieldResolver resolver = new LegacyDcCustomFieldResolver();
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
        var resolved = new LegacyDcAttachmentBundleResolver().resolve(zip, "30001", null);
        assertTrue(resolved.hasContent());
    }

    private static LegacyDcXmlParser.ParsedEntity issue(String key) {
        LegacyDcXmlParser.ParsedEntity e = new LegacyDcXmlParser.ParsedEntity();
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
