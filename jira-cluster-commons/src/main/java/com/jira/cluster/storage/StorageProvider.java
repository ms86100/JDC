package com.jira.cluster.storage;

import java.io.InputStream;
import java.time.Duration;

public interface StorageProvider {

    void store(String path, InputStream data, long size);

    InputStream retrieve(String path);

    void delete(String path);

    boolean exists(String path);

    String getUrl(String path, Duration validity);
}
