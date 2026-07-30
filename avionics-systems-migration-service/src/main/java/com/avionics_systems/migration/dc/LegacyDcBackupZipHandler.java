package com.avionics_systems.migration.dc;

import com.avionics_systems.migration.exception.MigrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts Legacy DC backup ZIP archives (entities.xml + data/attachments).
 */
@Component
@Slf4j
public class LegacyDcBackupZipHandler {

    private static final long MAX_UNCOMPRESSED_BYTES = 2L * 1024 * 1024 * 1024; // 2GB safety cap
    private static final int MAX_ENTRIES = 500_000;

    public ExtractedBackup extractZipToTemp(Path zipFile) throws IOException {
        Path extractRoot = Files.createTempDirectory("legacy-dc-backup-");
        long totalBytes = 0;
        int entries = 0;

        try (InputStream in = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entries > MAX_ENTRIES) {
                    throw new MigrationException("ZIP exceeds max entry count (" + MAX_ENTRIES + ")");
                }
                if (entry.isDirectory()) {
                    continue;
                }
                Path target = safeResolve(extractRoot, entry.getName());
                Files.createDirectories(target.getParent());
                long written = Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                totalBytes += written;
                if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                    throw new MigrationException("ZIP exceeds max uncompressed size");
                }
            }
        }

        Path entitiesXml = findEntitiesXml(extractRoot);
        Path attachmentsRoot = findAttachmentsRoot(extractRoot);
        log.info("Extracted DC backup: entities={}, attachments={}", entitiesXml, attachmentsRoot);
        return new ExtractedBackup(extractRoot, entitiesXml, attachmentsRoot);
    }

    private static Path findEntitiesXml(Path root) throws IOException {
        try (var walk = Files.walk(root)) {
            return walk.filter(p -> p.getFileName().toString().equalsIgnoreCase("entities.xml"))
                    .findFirst()
                    .orElseThrow(() -> new MigrationException("entities.xml not found in backup ZIP"));
        }
    }

    private static Path findAttachmentsRoot(Path root) {
        Path direct = root.resolve("data").resolve("attachments");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        Path alt = root.resolve("attachments");
        if (Files.isDirectory(alt)) {
            return alt;
        }
        return root.resolve("data/attachments");
    }

    static Path safeResolve(Path root, String entryName) throws IOException {
        String sanitized = LegacyDcAttachmentBundleResolver.sanitizeFileName(entryName.replace('\\', '/'));
        Path resolved = root.resolve(sanitized).normalize();
        Path normalizedRoot = root.toRealPath();
        if (!resolved.toAbsolutePath().startsWith(normalizedRoot)) {
            throw new MigrationException("Zip slip detected: " + entryName);
        }
        return resolved;
    }

    public void deleteExtracted(ExtractedBackup backup) {
        if (backup == null || backup.extractRoot() == null) {
            return;
        }
        try (var walk = Files.walk(backup.extractRoot())) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // ignore
                }
            });
        } catch (IOException e) {
            log.warn("Failed to cleanup extract dir: {}", e.getMessage());
        }
    }

    public record ExtractedBackup(Path extractRoot, Path entitiesXml, Path attachmentsRoot) {
    }
}
