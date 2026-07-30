package com.avionics_systems.migration.dc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Resolves attachment binaries from DC export zip sidecars (data/attachments/).
 */
@Component
@Slf4j
public class LegacyDcAttachmentBundleResolver {

    public ResolvedAttachment resolve(Path bundlePath, String attachmentId, String fileName) {
        if (bundlePath == null) {
            return ResolvedAttachment.empty();
        }
        try {
            if (Files.isDirectory(bundlePath)) {
                return resolveFromDirectory(bundlePath, attachmentId, fileName);
            }
            if (Files.isRegularFile(bundlePath) && bundlePath.toString().toLowerCase().endsWith(".zip")) {
                return resolveFromZip(bundlePath, attachmentId, fileName);
            }
        } catch (IOException e) {
            log.warn("Attachment bundle resolve failed: {}", e.getMessage());
        }
        return ResolvedAttachment.empty();
    }

    private ResolvedAttachment resolveFromDirectory(Path root, String attachmentId, String fileName) throws IOException {
        List<Path> candidates = candidatePaths(root, attachmentId, fileName);
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate) && isSafePath(root, candidate)) {
                byte[] bytes = Files.readAllBytes(candidate);
                String mime = Files.probeContentType(candidate);
                return new ResolvedAttachment(bytes, fileName != null ? fileName : candidate.getFileName().toString(),
                        mime, sha256(bytes));
            }
        }
        return ResolvedAttachment.empty();
    }

    private ResolvedAttachment resolveFromZip(Path zipPath, String attachmentId, String fileName) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            for (String entryPath : listCandidateZipEntries(attachmentId, fileName)) {
                ZipEntry entry = zip.getEntry(entryPath);
                if (entry == null) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    byte[] bytes = in.readAllBytes();
                    String resolvedName = fileName != null ? fileName : Paths.get(entryPath).getFileName().toString();
                    return new ResolvedAttachment(bytes, resolvedName, null, sha256(bytes));
                }
            }
        }
        return ResolvedAttachment.empty();
    }

    private static List<Path> candidatePaths(Path root, String attachmentId, String fileName) {
        List<Path> out = new ArrayList<>();
        if (attachmentId != null) {
            out.add(root.resolve(attachmentId));
            out.add(root.resolve("data/attachments").resolve(attachmentId));
            out.add(root.resolve("attachments").resolve(attachmentId));
        }
        if (fileName != null) {
            out.add(root.resolve(sanitizeFileName(fileName)));
            out.add(root.resolve("data/attachments").resolve(sanitizeFileName(fileName)));
        }
        return out;
    }

    private static List<String> listCandidateZipEntries(String attachmentId, String fileName) {
        List<String> out = new ArrayList<>();
        if (attachmentId != null) {
            out.add(attachmentId);
            out.add("data/attachments/" + attachmentId);
            out.add("attachments/" + attachmentId);
        }
        if (fileName != null) {
            String safe = sanitizeFileName(fileName);
            out.add(safe);
            out.add("data/attachments/" + safe);
        }
        return out;
    }

    static boolean isSafePath(Path root, Path candidate) throws IOException {
        Path normalizedRoot = root.toRealPath();
        Path normalized = candidate.toRealPath();
        return normalized.startsWith(normalizedRoot);
    }

    static String sanitizeFileName(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = name.replace("\\", "/");
        while (cleaned.contains("..")) {
            cleaned = cleaned.replace("..", "");
        }
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }

    private static String sha256(byte[] bytes) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public record ResolvedAttachment(byte[] content, String fileName, String mimeType, String checksum) {
        public static ResolvedAttachment empty() {
            return new ResolvedAttachment(new byte[0], null, null, null);
        }

        public boolean hasContent() {
            return content != null && content.length > 0;
        }
    }
}
