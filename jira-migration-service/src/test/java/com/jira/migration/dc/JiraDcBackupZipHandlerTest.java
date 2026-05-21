package com.jira.migration.dc;

import com.jira.migration.exception.MigrationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class JiraDcBackupZipHandlerTest {

    @TempDir
    Path tempDir;

    private final JiraDcBackupZipHandler handler = new JiraDcBackupZipHandler();

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

        JiraDcBackupZipHandler.ExtractedBackup extracted = handler.extractZipToTemp(zip);
        assertTrue(Files.exists(extracted.entitiesXml()));
        assertTrue(Files.isDirectory(extracted.attachmentsRoot()));
        handler.deleteExtracted(extracted);
        assertFalse(Files.exists(extracted.extractRoot()));
    }

    @Test
    void safeResolve_rejectsZipSlip() {
        assertThrows(MigrationException.class, () ->
                JiraDcBackupZipHandler.safeResolve(tempDir, "../../../etc/passwd"));
    }
}
