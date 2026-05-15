package com.jira.migration.service.attachment;

import com.jira.migration.config.storage.AttachmentStorageConfig;
import com.jira.migration.dto.attachment.AttachmentMetadata;
import com.jira.migration.dto.attachment.FileValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service for validating files before upload.
 * Performs file extension, MIME type, size, and magic byte validation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileValidationService {

    private final AttachmentStorageConfig config;

    // Magic byte patterns for common file types
    private static final Map<String, byte[][]> MAGIC_BYTES = Map.ofEntries(
            Map.entry("image/jpeg", new byte[][] {
                    new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0},
                    new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE1},
                    new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE8}
            }),
            Map.entry("image/png", new byte[][] {
                    new byte[] {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
            }),
            Map.entry("image/gif", new byte[][] {
                    new byte[] {(byte)0x47, 0x49, 0x46, 0x38, 0x37, 0x61},  // GIF87a
                    new byte[] {(byte)0x47, 0x49, 0x46, 0x38, 0x39, 0x61}   // GIF89a
            }),
            Map.entry("application/pdf", new byte[][] {
                    new byte[] {(byte)0x25, 0x50, 0x44, 0x46, 0x2D}  // %PDF-
            }),
            Map.entry("application/zip", new byte[][] {
                    new byte[] {(byte)0x50, 0x4B, 0x03, 0x04},  // PK..
                    new byte[] {(byte)0x50, 0x4B, 0x05, 0x06},  // Empty zip
                    new byte[] {(byte)0x50, 0x4B, 0x07, 0x08}   // Spanned zip
            }),
            Map.entry("application/x-zip-compressed", new byte[][] {
                    new byte[] {(byte)0x50, 0x4B, 0x03, 0x04}
            }),
            Map.entry("text/plain", new byte[][] {
                    // ASCII text - no high bytes
            }),
            Map.entry("text/csv", new byte[][] {
                    // CSV is text, no specific magic bytes
            })
    );

    // Suspicious patterns to detect
    private static final List<byte[]> SUSPICIOUS_PATTERNS = List.of(
            // Double extensions (common malware pattern)
            // HTML/JavaScript embedded in images
            // Executable scripts
    );

    /**
     * Validate a file with all available information.
     *
     * @param fileName    The original file name
     * @param mimeType    The declared MIME type
     * @param size        The file size in bytes
     * @param headerBytes The first bytes of the file for magic byte validation
     * @return Validation result
     */
    public FileValidationResult validateFile(String fileName, String mimeType,
                                              long size, byte[] headerBytes) {
        FileValidationResult result = FileValidationResult.success();

        // 1. Validate file name
        validateFileName(fileName, result);

        // 2. Validate file size
        validateFileSize(size, result);

        // 3. Validate MIME type
        validateMimeType(mimeType, result);

        // 4. Validate magic bytes
        String detectedMimeType = validateMagicBytes(headerBytes, mimeType, result);

        // 5. Check for suspicious content
        checkSuspiciousContent(headerBytes, result);

        // 6. Detect actual MIME type from content
        if (detectedMimeType != null && detectedMimeType.equals("application/octet-stream")) {
            result.setDetectedMimeType(detectMimeTypeFromContent(headerBytes));
        }

        // Set final validity
        result.setValid(result.getErrors().isEmpty());

        return result;
    }

    /**
     * Quick validation for files without magic byte check.
     */
    public FileValidationResult validateFile(String fileName, String mimeType, long size) {
        FileValidationResult result = FileValidationResult.success();

        validateFileName(fileName, result);
        validateFileSize(size, result);
        validateMimeType(mimeType, result);

        result.setValid(result.getErrors().isEmpty());

        return result;
    }

    /**
     * Check if a MIME type is allowed.
     */
    public boolean isAllowedMimeType(String mimeType) {
        return config.isAllowedMimeType(mimeType);
    }

    /**
     * Check if a file extension is allowed.
     */
    public boolean isAllowedExtension(String fileName) {
        String extension = getFileExtension(fileName);
        if (extension.isEmpty()) {
            return false;
        }

        // Check against known allowed extensions
        Set<String> allowedExtensions = Set.of(
                "jpg", "jpeg", "png", "gif", "webp", "svg",
                "pdf", "zip", "7z", "rar",
                "txt", "csv", "json", "xml",
                "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                "html", "css", "js", "ts"
        );

        return allowedExtensions.contains(extension.toLowerCase());
    }

    private void validateFileName(String fileName, FileValidationResult result) {
        if (fileName == null || fileName.isBlank()) {
            result.addError("File name is required");
            return;
        }

        if (fileName.length() > 255) {
            result.addError("File name exceeds maximum length of 255 characters");
        }

        // Check for path traversal attempts
        if (fileName.contains("..") || fileName.startsWith("/") || fileName.startsWith("\\")) {
            result.addError("Invalid file name containing path traversal sequence");
            result.setSuspicious(true);
        }

        // Check for null bytes
        if (fileName.contains("\0")) {
            result.addError("Invalid file name containing null byte");
            result.setSuspicious(true);
        }

        // Warn about suspicious names
        String lowerName = fileName.toLowerCase();
        if (lowerName.contains("exe") || lowerName.contains("bat") ||
                lowerName.contains("cmd") || lowerName.contains("sh") ||
                lowerName.contains("ps1") || lowerName.contains("scr")) {
            result.addWarning("File has executable extension - will be validated further");
        }
    }

    private void validateFileSize(long size, FileValidationResult result) {
        long maxSize = config.getMaxFileSizeBytes();
        if (size <= 0) {
            result.addError("File size must be greater than 0");
            return;
        }

        if (size > maxSize) {
            result.addError(String.format("File size %d bytes exceeds maximum allowed %d bytes",
                    size, maxSize));
        }

        // Warn for very large files (but still within limit)
        if (size > maxSize * 0.8) {
            result.addWarning("File size is close to the maximum allowed limit");
        }
    }

    private void validateMimeType(String mimeType, FileValidationResult result) {
        if (mimeType == null || mimeType.isBlank()) {
            result.addError("MIME type is required");
            return;
        }

        if (!isAllowedMimeType(mimeType)) {
            result.addError("MIME type '" + mimeType + "' is not allowed");
        }
    }

    private String validateMagicBytes(byte[] header, String declaredMimeType,
                                       FileValidationResult result) {
        if (header == null || header.length < 4) {
            result.addWarning("File too small for magic byte validation");
            return null;
        }

        String detectedType = null;

        // Check against magic bytes for declared type
        if (declaredMimeType != null && MAGIC_BYTES.containsKey(declaredMimeType)) {
            if (!matchesMagicBytes(header, MAGIC_BYTES.get(declaredMimeType))) {
                result.addWarning("File content does not match declared MIME type '" +
                        declaredMimeType + "'");
                result.setSuggestedMimeType(detectMimeTypeFromContent(header));
            }
            detectedType = declaredMimeType;
        } else {
            detectedType = detectMimeTypeFromContent(header);
        }

        // Verify detected type is allowed
        if (detectedType != null && !detectedType.equals("application/octet-stream")) {
            if (!isAllowedMimeType(detectedType)) {
                result.addError("Detected file type '" + detectedType + "' is not allowed");
            }
        }

        return detectedType;
    }

    private String detectMimeTypeFromContent(byte[] header) {
        if (header == null || header.length < 4) {
            return "application/octet-stream";
        }

        for (Map.Entry<String, byte[][]> entry : MAGIC_BYTES.entrySet()) {
            if (matchesMagicBytes(header, entry.getValue())) {
                return entry.getKey();
            }
        }

        // Check for text content (no high bytes in first several bytes)
        boolean isText = true;
        for (int i = 0; i < Math.min(header.length, 512); i++) {
            byte b = header[i];
            // Allow common text characters
            if ((b < 32 && b != 9 && b != 10 && b != 13) || b == 127) {
                isText = false;
                break;
            }
        }
        if (isText) {
            return "text/plain";
        }

        return "application/octet-stream";
    }

    private boolean matchesMagicBytes(byte[] header, byte[][] patterns) {
        for (byte[] pattern : patterns) {
            if (matchesPattern(header, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPattern(byte[] header, byte[] pattern) {
        if (header.length < pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (header[i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private void checkSuspiciousContent(byte[] header, FileValidationResult result) {
        if (header == null || header.length < 10) {
            return;
        }

        // Check for HTML/JavaScript embedded in files (XSS vector)
        boolean looksLikeHtml = false;
        String headerStr = new String(header, 0, Math.min(header.length, 1000));

        if (headerStr.contains("<!DOCTYPE html") || headerStr.contains("<html") ||
                headerStr.contains("<script") || headerStr.contains("<?xml")) {
            looksLikeHtml = true;
        }

        // Check for PHP or other script tags
        if (headerStr.contains("<?php") || headerStr.contains("<?")) {
            result.addError("File appears to contain executable script code");
            result.setSuspicious(true);
        }

        // If file claims to be an image but contains HTML
        if (looksLikeHtml && !headerStr.trim().startsWith("<")) {
            result.addWarning("File may contain embedded HTML or script content");
            result.setSuspicious(true);
        }

        // Check for executable headers
        if (header[0] == 0x4D && header[1] == 0x5A) {  // MZ - Windows executable
            result.addError("File appears to be a Windows executable");
            result.setSuspicious(true);
        }

        if (header[0] == 0x7F && header[1] == 0x45 && header[2] == 0x4C && header[3] == 0x46) {  // ELF
            result.addError("File appears to be a Linux executable");
            result.setSuspicious(true);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }
}