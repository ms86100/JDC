package com.jira.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves global issue status IDs to display metadata (Jira DC status catalog).
 */
@Component
@Slf4j
public class WorkflowStatusCatalog {

    private static final Map<String, String> KNOWN_STATUS_NAMES = Map.ofEntries(
            Map.entry("00000000-0000-0000-0001-000000000001", "Backlog"),
            Map.entry("00000000-0000-0000-0001-000000000002", "To Do"),
            Map.entry("00000000-0000-0000-0001-000000000003", "In Progress"),
            Map.entry("00000000-0000-0000-0001-000000000004", "In Review"),
            Map.entry("00000000-0000-0000-0001-000000000005", "Done"),
            Map.entry("00000000-0000-0000-0001-000000000006", "Open"),
            Map.entry("00000000-0000-0000-0001-000000000007", "Resolved"),
            Map.entry("00000000-0000-0000-0001-000000000008", "Closed"),
            Map.entry("00000000-0000-0000-0001-000000000009", "Defined")
    );

    private static final Map<String, String> CATEGORY_COLORS = Map.of(
            "TODO", "#6C757D",
            "IN_PROGRESS", "#FF991F",
            "DONE", "#00875A"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${jira.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    @Value("${jira.services.admin-url:http://localhost:8093}")
    private String adminServiceUrl;

    public Map<String, StatusMeta> loadCatalog() {
        Map<String, StatusMeta> catalog = new HashMap<>();
        mergeIssueStatuses(catalog);
        mergeAdminStatuses(catalog);
        KNOWN_STATUS_NAMES.forEach((id, name) ->
                catalog.putIfAbsent(id, new StatusMeta(name, inferCategory(name), colorFor(inferCategory(name))))
        );
        return catalog;
    }

    public String resolveName(UUID statusId, Map<String, StatusMeta> catalog) {
        if (statusId == null) {
            return "—";
        }
        String key = statusId.toString();
        StatusMeta meta = catalog.get(key);
        if (meta != null) {
            return meta.name();
        }
        return KNOWN_STATUS_NAMES.getOrDefault(key, key);
    }

    public StatusMeta resolve(UUID statusId, Map<String, StatusMeta> catalog) {
        if (statusId == null) {
            return new StatusMeta("—", "TODO", "#6C757D");
        }
        String key = statusId.toString();
        StatusMeta meta = catalog.get(key);
        if (meta != null) {
            return meta;
        }
        String name = KNOWN_STATUS_NAMES.getOrDefault(key, key);
        String category = inferCategory(name);
        return new StatusMeta(name, category, colorFor(category));
    }

    private void mergeIssueStatuses(Map<String, StatusMeta> catalog) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    issueServiceUrl + "/api/issues/statuses",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getBody() == null) {
                return;
            }
            for (Map<String, Object> row : response.getBody()) {
                putRow(catalog, row, "category", null);
            }
        } catch (Exception e) {
            log.warn("Failed to load issue statuses catalog: {}", e.getMessage());
        }
    }

    private void mergeAdminStatuses(Map<String, StatusMeta> catalog) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    adminServiceUrl + "/api/admin/statuses",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getBody() == null) {
                return;
            }
            for (Map<String, Object> row : response.getBody()) {
                putRow(catalog, row, "statusCategory", "statusColor");
            }
        } catch (Exception e) {
            log.warn("Failed to load admin statuses catalog: {}", e.getMessage());
        }
    }

    private void putRow(Map<String, StatusMeta> catalog, Map<String, Object> row, String categoryKey, String colorKey) {
        String id = stringVal(row.get("id"));
        String name = stringVal(row.get("name"));
        String category = stringVal(row.get(categoryKey));
        String color = colorKey != null ? stringVal(row.get(colorKey)) : null;
        if (id != null && name != null) {
            catalog.put(id, new StatusMeta(name, category, color != null ? color : colorFor(category)));
        }
    }

    private static String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static String inferCategory(String name) {
        if (name == null) {
            return "TODO";
        }
        String lower = name.toLowerCase();
        if (lower.contains("done") || lower.contains("closed") || lower.contains("resolved")) {
            return "DONE";
        }
        if (lower.contains("progress") || lower.contains("review")) {
            return "IN_PROGRESS";
        }
        return "TODO";
    }

    static String colorFor(String category) {
        return CATEGORY_COLORS.getOrDefault(category, "#6C757D");
    }

    public record StatusMeta(String name, String category, String color) {}
}
