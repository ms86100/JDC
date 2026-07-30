package com.avionics_systems.migration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.avionics_systems.migration.service.clients.IssueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves Legacy DC display names (Story, High, …) to issue-service UUIDs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueReferenceResolver {

    private final IssueServiceClient issueServiceClient;

    private volatile Map<String, UUID> issueTypesByName = Map.of();
    private volatile Map<String, UUID> prioritiesByName = Map.of();

    public UUID resolveIssueTypeId(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        ensureCaches();
        String key = name.trim().toLowerCase(Locale.ROOT);
        UUID id = issueTypesByName.get(key);
        if (id == null) {
            log.warn("Unknown issue type '{}', issue-service will use default", name);
        }
        return id;
    }

    public UUID resolvePriorityId(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        ensureCaches();
        String key = name.trim().toLowerCase(Locale.ROOT);
        UUID id = prioritiesByName.get(key);
        if (id == null) {
            log.warn("Unknown priority '{}', issue-service will use default", name);
        }
        return id;
    }

    private void ensureCaches() {
        if (!issueTypesByName.isEmpty() && !prioritiesByName.isEmpty()) {
            return;
        }
        synchronized (this) {
            if (!issueTypesByName.isEmpty() && !prioritiesByName.isEmpty()) {
                return;
            }
            issueTypesByName = loadNameMap(issueServiceClient.listIssueTypes(), "name");
            prioritiesByName = loadNameMap(issueServiceClient.listPriorities(), "name");
            log.info("Loaded {} issue types and {} priorities for CSV import",
                    issueTypesByName.size(), prioritiesByName.size());
        }
    }

    private Map<String, UUID> loadNameMap(List<JsonNode> nodes, String nameField) {
        Map<String, UUID> map = new HashMap<>();
        for (JsonNode node : nodes) {
            if (node == null || !node.has("id")) {
                continue;
            }
            String idStr = node.get("id").asText(null);
            String name = node.has(nameField) ? node.get(nameField).asText(null) : null;
            if (idStr == null || name == null || name.isBlank()) {
                continue;
            }
            try {
                map.put(name.trim().toLowerCase(Locale.ROOT), UUID.fromString(idStr));
            } catch (IllegalArgumentException e) {
                log.debug("Skipping invalid id in {}: {}", nameField, idStr);
            }
        }
        return Map.copyOf(map);
    }
}
