package com.avionics_systems.migration.dc;

import com.avionics_systems.migration.exception.MigrationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDcBackupZipHandlerTest {

    @TempDir
    Path tempDir;

    private final LegacyDcBackupZipHandler handler = new LegacyDcBackupZipHandler();

    @Test
    void extractZip_findsEntitiesXmlAndAttachments() throws IOException {
        Path zip = tempDir.resolve("backup.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("entities.xml"));
            zos.write("<entities></entities>".getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("data/attachments/10000/photo.png"));
            zos.write(new byte[] { 1, 2, 3 });
            zos.closeEntry();
        }

        LegacyDcBackupZipHandler.ExtractedBackup extracted = handler.extractZipToTemp(zip);
        assertTrue(Files.exists(extracted.entitiesXml()));
        assertTrue(Files.isDirectory(extracted.attachmentsRoot()));
        handler.deleteExtracted(extracted);
        assertFalse(Files.exists(extracted.extractRoot()));
    }

    @Test
    void safeResolve_rejectsZipSlip() {
        assertThrows(MigrationException.class, () ->
                LegacyDcBackupZipHandler.safeResolve(tempDir, "../../../etc/passwd"));
    }
}
