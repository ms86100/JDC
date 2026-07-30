package com.avionics_systems.migration.dc;

import com.avionics_systems.migration.parser.LegacyDcEntitiesXmlParser;
import com.avionics_systems.migration.parser.LegacyDcXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Backup ZIP → entities.xml parse → validate (prepareValidate path).
 */
class LegacyDcBackupZipIntegrationTest {

    @TempDir
    Path tempDir;

    private final LegacyDcBackupZipHandler zipHandler = new LegacyDcBackupZipHandler();
    private final LegacyDcImportOrchestrator orchestrator = new LegacyDcImportOrchestrator(
            new LegacyDcXmlParser(null, null, null, null, null),
            null,
            new LegacyDcImportValidationService(null, null, null),
            new LegacyDcCustomFieldResolver(),
            zipHandler);

    @Test
    void enterpriseBackupZip_prepareValidate_parsesEntities() throws Exception {
        Path zip = buildEnterpriseBackupZip();
        LegacyDcImportOrchestrator.ResolvedInputs resolved = orchestrator.resolveInputs(zip, null, true);
        try {
            assertTrue(Files.exists(resolved.xmlPath()));
            assertTrue(resolved.xmlPath().getFileName().toString().equalsIgnoreCase("entities.xml"));

            var prep = orchestrator.prepareValidate(
                    resolved.xmlPath(),
                    resolved.attachmentBundlePath(),
                    Map.of("blockOnValidationErrors", false));

            assertTrue(prep.parseResult().getTotalEntities() > 0);
            assertTrue(prep.validationReport().valid(), () -> "blockers: " + prep.validationReport().blockerCount());

            Map<String, Long> byType = prep.parseResult().getEntities().stream()
                    .collect(Collectors.groupingBy(LegacyDcXmlParser.ParsedEntity::getEntityType, Collectors.counting()));
            assertTrue(byType.getOrDefault("Issue", 0L) >= 1);
            assertTrue(byType.getOrDefault("Worklog", 0L) >= 1);
        } finally {
            if (resolved.extractedBackup() != null) {
                orchestrator.cleanupExtracted(resolved.extractedBackup());
            }
        }
    }

    @Test
    void enterpriseBackupZip_extractThenParse_matchesDirectParse() throws Exception {
        Path zip = buildEnterpriseBackupZip();
        LegacyDcBackupZipHandler.ExtractedBackup extracted = zipHandler.extractZipToTemp(zip);
        try {
            var fromZip = LegacyDcEntitiesXmlParser.parse(extracted.entitiesXml());
            Path direct = copyFixture("enterprise-dc-entities.xml");
            var directParse = LegacyDcEntitiesXmlParser.parse(direct);
            assertEquals(directParse.size(), fromZip.size());
        } finally {
            zipHandler.deleteExtracted(extracted);
        }
    }

    private Path buildEnterpriseBackupZip() throws Exception {
        Path entities = copyFixture("enterprise-dc-entities.xml");
        Path zip = tempDir.resolve("enterprise-backup.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("entities.xml"));
            zos.write(Files.readAllBytes(entities));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("data/attachments/10000/doc.txt"));
            zos.write("attachment-bytes".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return zip;
    }

    private Path copyFixture(String name) throws Exception {
        String resource = "/samples/issues/" + name;
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            assertNotNull(in, "Missing: " + resource);
            Path dest = tempDir.resolve(name);
            Files.write(dest, in.readAllBytes());
            return dest;
        }
    }
}
