package com.avionics_systems.migration.dc;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-job catalog of DC reference entities (status, type, component, version) for import ordering.
 */
@Component
public class LegacyDcReferenceCatalog {

    private final Map<UUID, Map<String, String>> catalogs = new ConcurrentHashMap<>();

    public void record(UUID jobId, String entityType, String entityKey, Map<String, String> fields) {
        if (jobId == null || entityType == null) {
            return;
        }
        String key = entityType + ":" + (entityKey != null ? entityKey : "");
        catalogs.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>()).put(key, summarize(fields));
    }

    public boolean hasCatalog(UUID jobId) {
        return jobId != null && catalogs.containsKey(jobId);
    }

    public int size(UUID jobId) {
        Map<String, String> m = catalogs.get(jobId);
        return m != null ? m.size() : 0;
    }

    public void clear(UUID jobId) {
        if (jobId != null) {
            catalogs.remove(jobId);
        }
    }

    private static String summarize(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        String name = fields.getOrDefault("name", fields.get("id"));
        return name != null ? name : fields.toString();
    }
}
