package com.avionics_systems.workflow.service;

import com.avionics_systems.cluster.util.StatusCategoryHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.avionics_systems.workflow.entity.WorkflowStatusDefinition;
import com.avionics_systems.workflow.repository.WorkflowStatusDefinitionRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves global issue status IDs to display metadata (Avionics Systems DC status catalog).
 *
 * The catalog is loaded from two upstream services (issue-service and admin-service) and
 * supplemented with a small static set of well-known DC statuses. To avoid making those
 * HTTP calls on every workflow detail render, the merged result is cached in-process for
 * a configurable TTL and refreshed lazily.
 */
@Component
@Slf4j
public class WorkflowStatusCatalog {

    @Value("${app.workflow.status-catalog.cache-ttl-ms:300000}")
    private long cacheTtlMs;

    private volatile Map<String, StatusMeta> cachedCatalog = new HashMap<>();
    private volatile long cachedAt = 0L;
    private volatile boolean degraded = false;

    public boolean isDegraded() { return degraded; }

    @Value("${app.workflow.status-catalog.known-status-names:00000000-0000-0000-0001-000000000001=Backlog,00000000-0000-0000-0001-000000000002=To Do,00000000-0000-0000-0001-000000000003=In Progress,00000000-0000-0000-0001-000000000004=In Review,00000000-0000-0000-0001-000000000005=Done,00000000-0000-0000-0001-000000000006=Open,00000000-0000-0000-0001-000000000007=Resolved,00000000-0000-0000-0001-000000000008=Closed,00000000-0000-0000-0001-000000000009=Defined}")
    private String knownStatusNamesStr;

    @Value("${app.workflow.status-catalog.color-todo:#6C757D}")
    private String colorTodo;

    @Value("${app.workflow.status-catalog.color-in-progress:#FF991F}")
    private String colorInProgress;

    @Value("${app.workflow.status-catalog.color-done:#00875A}")
    private String colorDone;

    @Value("${app.workflow.status-catalog.null-status-color:#6C757D}")
    private String nullStatusColor;

    @Value("${app.workflow.status-catalog.legacy-filter-suffix:(legacy)}")
    private String legacyFilterSuffix;

    private final RestTemplate restTemplate;
    private final WorkflowStatusDefinitionRepository statusDefinitionRepository;

    @Value("${avionics-systems.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    @Value("${avionics-systems.services.admin-url:http://localhost:8093}")
    private String adminServiceUrl;

    private Map<String, String> getKnownStatusNames() {
        Map<String, String> result = new HashMap<>();
        if (knownStatusNamesStr != null && !knownStatusNamesStr.isBlank()) {
            for (String entry : knownStatusNamesStr.split(",(?=[0-9a-fA-F]{8}-[0-9a-fA-F]{4})")) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    result.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return result;
    }

    private Map<String, String> getCategoryColors() {
        return Map.of(
                "TODO", colorTodo,
                "IN_PROGRESS", colorInProgress,
                "DONE", colorDone
        );
    }

    public WorkflowStatusCatalog(RestTemplate restTemplate,
                                WorkflowStatusDefinitionRepository statusDefinitionRepository) {
        this.restTemplate = restTemplate;
        this.statusDefinitionRepository = statusDefinitionRepository;
    }

    public Map<String, StatusMeta> loadCatalog() {
        long now = System.currentTimeMillis();
        if (!cachedCatalog.isEmpty() && (now - cachedAt) < cacheTtlMs) {
            return cachedCatalog;
        }
        synchronized (this) {
            if (!cachedCatalog.isEmpty() && (System.currentTimeMillis() - cachedAt) < cacheTtlMs) {
                return cachedCatalog;
            }
            degraded = false;
            Map<String, StatusMeta> catalog = new HashMap<>();
            mergeLocalDefinitions(catalog);
            mergeIssueStatuses(catalog);
            mergeAdminStatuses(catalog);
            getKnownStatusNames().forEach((id, name) ->
                    catalog.putIfAbsent(id, new StatusMeta(name, inferCategory(name), colorFor(inferCategory(name))))
            );
            cachedCatalog = catalog;
            cachedAt = System.currentTimeMillis();
            return cachedCatalog;
        }
    }

    /**
     * Invalidate the in-memory catalog. Call after admin updates the status list so subsequent
     * loads pick up the change instead of waiting for the TTL to expire.
     */
    public void invalidate() {
        synchronized (this) {
            cachedCatalog = new HashMap<>();
            cachedAt = 0L;
        }
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
        return getKnownStatusNames().getOrDefault(key, key);
    }

    public StatusMeta resolve(UUID statusId, Map<String, StatusMeta> catalog) {
        if (statusId == null) {
            return new StatusMeta("—", "TODO", nullStatusColor);
        }
        String key = statusId.toString();
        StatusMeta meta = catalog.get(key);
        if (meta != null) {
            return meta;
        }
        String name = getKnownStatusNames().getOrDefault(key, key);
        String category = inferCategory(name);
        return new StatusMeta(name, category, colorFor(category));
    }

    private void mergeLocalDefinitions(Map<String, StatusMeta> catalog) {
        try {
            List<WorkflowStatusDefinition> definitions = statusDefinitionRepository.findAll();
            for (WorkflowStatusDefinition def : definitions) {
                String id = def.getStatusId().toString();
                String name = def.getName();
                String category = def.getCategory() != null ? def.getCategory() : inferCategory(name);
                String color = def.getColor() != null ? def.getColor() : colorFor(category);
                catalog.putIfAbsent(id, new StatusMeta(name, category, color));
            }
        } catch (Exception e) {
            log.warn("Failed to load local workflow status definitions: {}", e.getMessage());
        }
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
                String name = stringVal(row.get("name"));
                if (name != null && legacyFilterSuffix != null
                        && !legacyFilterSuffix.isBlank() && name.contains(legacyFilterSuffix)) {
                    continue;
                }
                putRow(catalog, row, "category", null);
            }
        } catch (Exception e) {
            log.warn("Failed to load issue statuses catalog: {}", e.getMessage());
            degraded = true;
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
            degraded = true;
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
        return StatusCategoryHelper.getCategory(name);
    }

    String colorFor(String category) {
        return getCategoryColors().getOrDefault(category, nullStatusColor);
    }

    public record StatusMeta(String name, String category, String color) {}
}
