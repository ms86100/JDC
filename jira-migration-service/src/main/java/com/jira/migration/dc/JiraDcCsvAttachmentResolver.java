package com.jira.migration.dc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolves CSV attachment references per Jira DC External System Import:
 * HTTP/HTTPS URLs, local paths, and {@code FILE:} under import attachments directory.
 */
@Component
@Slf4j
public class JiraDcCsvAttachmentResolver {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${migration.import.attachments-dir:}")
    private String attachmentsImportDir;

    @Value("${migration.attachment.max-size-bytes:10485760}")
    private long maxAttachmentSizeBytes;

    public byte[] resolveContent(String reference) throws IOException {
        if (reference == null || reference.isBlank()) {
            return new byte[0];
        }
        String trimmed = reference.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("http://")
                || trimmed.toLowerCase(Locale.ROOT).startsWith("https://")) {
            return fetchUrl(trimmed);
        }
        if (trimmed.toUpperCase(Locale.ROOT).startsWith("FILE:")) {
            return readFile(resolveFilePath(trimmed.substring(5).trim()));
        }
        return readFile(Path.of(trimmed));
    }

    public List<AttachmentRef> parseAttachmentList(String cellValue) {
        List<AttachmentRef> refs = new ArrayList<>();
        if (cellValue == null || cellValue.isBlank()) {
            return refs;
        }
        String[] parts = cellValue.split("[;|]");
        for (String part : parts) {
            String p = part.trim();
            if (!p.isBlank()) {
                refs.add(new AttachmentRef(p, fileNameFromReference(p)));
            }
        }
        return refs;
    }

    public record AttachmentRef(String reference, String fileName) {}

    private byte[] fetchUrl(String url) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode() + " for " + url);
            }
            byte[] body = response.body();
            if (body != null && body.length > maxAttachmentSizeBytes) {
                throw new IOException("Attachment exceeds max size " + maxAttachmentSizeBytes);
            }
            return body != null ? body : new byte[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted: " + url, e);
        }
    }

    private Path resolveFilePath(String relativeOrAbsolute) {
        Path p = Path.of(relativeOrAbsolute);
        if (p.isAbsolute() && Files.exists(p)) {
            return p;
        }
        if (attachmentsImportDir != null && !attachmentsImportDir.isBlank()) {
            Path base = Path.of(attachmentsImportDir).normalize().toAbsolutePath();
            Path resolved = base.resolve(relativeOrAbsolute).normalize();
            if (!resolved.startsWith(base)) {
                throw new SecurityException("Path traversal blocked: " + relativeOrAbsolute);
            }
            return resolved;
        }
        return p;
    }

    private byte[] readFile(Path path) throws IOException {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return new byte[0];
        }
        long size = Files.size(path);
        if (size > maxAttachmentSizeBytes) {
            throw new IOException("File exceeds max size: " + path);
        }
        return Files.readAllBytes(path);
    }

    private String fileNameFromReference(String ref) {
        if (ref.toUpperCase(Locale.ROOT).startsWith("FILE:")) {
            ref = ref.substring(5).trim();
        }
        try {
            if (ref.contains("://")) {
                String path = URI.create(ref).getPath();
                if (path != null && path.contains("/")) {
                    return path.substring(path.lastIndexOf('/') + 1);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        int slash = Math.max(ref.lastIndexOf('/'), ref.lastIndexOf('\\'));
        return slash >= 0 ? ref.substring(slash + 1) : ref;
    }
}
