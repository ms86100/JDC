package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe in-memory file system for scripts. No actual disk I/O — all data lives
 * in a ConcurrentHashMap scoped to the script execution. Files are lost when
 * the script finishes. Use for temp data processing, CSV generation, log
 * accumulation, etc.
 */
@Slf4j
public class JdcFileApi {

    private static final long MAX_TOTAL_SIZE = 10 * 1024 * 1024; // 10MB total
    private static final int MAX_FILES = 100;

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private long totalSize = 0;

    @HostAccess.Export
    public boolean writeFile(String path, String content) {
        try {
            if (path == null || content == null) return false;
            String safePath = sanitizePath(path);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            if (totalSize + data.length > MAX_TOTAL_SIZE) {
                log.warn("File write rejected — would exceed 10MB total limit");
                return false;
            }
            if (files.size() >= MAX_FILES && !files.containsKey(safePath)) {
                log.warn("File write rejected — max {} files reached", MAX_FILES);
                return false;
            }
            byte[] old = files.put(safePath, data);
            totalSize += data.length - (old != null ? old.length : 0);
            return true;
        } catch (Exception e) {
            log.warn("writeFile failed: {}", e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public String readFile(String path) {
        try {
            if (path == null) return null;
            byte[] data = files.get(sanitizePath(path));
            return data != null ? new String(data, StandardCharsets.UTF_8) : null;
        } catch (Exception e) {
            log.warn("readFile failed: {}", e.getMessage());
            return null;
        }
    }

    @HostAccess.Export
    public boolean fileExists(String path) {
        return path != null && files.containsKey(sanitizePath(path));
    }

    @HostAccess.Export
    public boolean deleteFile(String path) {
        try {
            if (path == null) return false;
            byte[] removed = files.remove(sanitizePath(path));
            if (removed != null) {
                totalSize -= removed.length;
                return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    @HostAccess.Export
    public List<String> listFiles() {
        return new ArrayList<>(files.keySet());
    }

    @HostAccess.Export
    public List<String> listFiles(String prefix) {
        if (prefix == null) return listFiles();
        String safePrefix = sanitizePath(prefix);
        return files.keySet().stream()
                .filter(k -> k.startsWith(safePrefix))
                .sorted()
                .toList();
    }

    @HostAccess.Export
    public int getFileSize(String path) {
        if (path == null) return -1;
        byte[] data = files.get(sanitizePath(path));
        return data != null ? data.length : -1;
    }

    @HostAccess.Export
    public boolean appendToFile(String path, String content) {
        try {
            if (path == null || content == null) return false;
            String safePath = sanitizePath(path);
            byte[] existing = files.getOrDefault(safePath, new byte[0]);
            byte[] addition = content.getBytes(StandardCharsets.UTF_8);
            if (totalSize + addition.length > MAX_TOTAL_SIZE) return false;
            byte[] combined = new byte[existing.length + addition.length];
            System.arraycopy(existing, 0, combined, 0, existing.length);
            System.arraycopy(addition, 0, combined, existing.length, addition.length);
            files.put(safePath, combined);
            totalSize += addition.length;
            return true;
        } catch (Exception e) { return false; }
    }

    private String sanitizePath(String path) {
        return path.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\.\\.", "_")
                   .trim();
    }
}
