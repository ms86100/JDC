package com.jira.migration.dc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.parser.JiraDcEntitiesXmlParser;
import com.jira.migration.parser.JiraDcXmlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Production-style DC backup ZIP (nested export/entities.xml + attachment tree).
 */
class JiraDcProductionBackupFixtureTest {

    @TempDir
    Path tempDir;

    private final JiraDcBackupZipHandler zipHandler = new JiraDcBackupZipHandler();
    private final JiraDcImportOrchestrator orchestrator = new JiraDcImportOrchestrator(
            new JiraDcXmlParser(null, null, null, null, null),
            null,
            new JiraDcImportValidationService(null, null, null),
            new JiraDcCustomFieldResolver(),
            zipHandler);

    @Test
    void manifest_listsExpectedDcLayout() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/samples/issues/production-dc-backup-manifest.json")) {
            assertNotNull(in);
            @SuppressWarnings("unchecked")
            Map<String, Object> manifest = new ObjectMapper().readValue(in, Map.class);
            @SuppressWarnings("unchecked")
            List<String> paths = (List<String>) ((Map<?, ?>) manifest.get("layout")).get("entitiesXml");
            assertTrue(paths.contains("export/entities.xml"));
        }
    }

    @Test
    void nestedExportZip_prepareValidate_matchesManifest() throws Exception {
        Path zip = buildProductionStyleZip();
        var resolved = orchestrator.resolveInputs(zip, null, true);
        try {
            assertTrue(resolved.xmlPath().toString().replace('\\', '/').contains("entities.xml"));
            var prep = orchestrator.prepareValidate(
                    resolved.xmlPath(), resolved.attachmentBundlePath(), Map.of());
            assertTrue(prep.validationReport().valid());
            assertTrue(prep.parseResult().getEntities().stream()
                    .anyMatch(e -> "User".equals(e.getEntityType())));
            assertTrue(Files.isDirectory(resolved.attachmentBundlePath())
                    || Files.exists(resolved.attachmentBundlePath()));
        } finally {
            if (resolved.extractedBackup() != null) {
                orchestrator.cleanupExtracted(resolved.extractedBackup());
            }
        }
    }

    @Test
    void productionEntities_parseAllCoreTypes() throws Exception {
        Path file = copyResource("production-dc-entities.xml");
        var entities = JiraDcEntitiesXmlParser.parse(file);
        assertTrue(entities.stream().anyMatch(e -> "User".equals(e.getEntityType())));
        assertTrue(entities.stream().anyMatch(e -> "Resolution".equals(e.getEntityType())));
        assertTrue(entities.stream().anyMatch(e -> "Watcher".equals(e.getEntityType())));
    }

    private Path buildProductionStyleZip() throws Exception {
        Path entities = copyResource("production-dc-entities.xml");
        Path zip = tempDir.resolve("production-dc-backup.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("export/exportDescriptor.properties"));
            zos.write("exportDate=2026-01-01\nexportType=ENTITY_XML".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("export/entities.xml"));
            zos.write(Files.readAllBytes(entities));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("data/attachments/PRD/10001/spec.pdf"));
            zos.write("%PDF-1.4 production-fixture".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return zip;
    }

    private Path copyResource(String name) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/samples/issues/" + name)) {
            assertNotNull(in, name);
            Path dest = tempDir.resolve(name);
            Files.write(dest, in.readAllBytes());
            return dest;
        }
    }
}
