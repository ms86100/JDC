package com.jira.cluster.storage;

import com.jira.cluster.config.ClusterProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Slf4j
public class LocalStorageProvider implements StorageProvider {

    private final Path basePath;

    public LocalStorageProvider(ClusterProperties properties) {
        this.basePath = Paths.get(properties.getStorage().getBasePath());
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage directory: " + basePath, e);
        }
    }

    @Override
    public void store(String path, InputStream data, long size) {
        try {
            Path target = basePath.resolve(path);
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file at {}", target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store: " + path, e);
        }
    }

    @Override
    public InputStream retrieve(String path) {
        try {
            return Files.newInputStream(basePath.resolve(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to retrieve: " + path, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(basePath.resolve(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete: " + path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(basePath.resolve(path));
    }

    @Override
    public String getUrl(String path, Duration validity) {
        return basePath.resolve(path).toUri().toString();
    }
}
