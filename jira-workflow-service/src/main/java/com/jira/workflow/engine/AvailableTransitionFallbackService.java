package com.jira.workflow.engine;

import com.jira.workflow.dto.AvailableTransitionResponse;
import com.jira.workflow.entity.Workflow;
import com.jira.workflow.service.WorkflowStatusCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * When no workflow transitions match the issue's current status, expose catalog status moves
 * so the UI is usable in dev and after scheme/status ID drift.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AvailableTransitionFallbackService {

    private final WorkflowIntegrationClient integrationClient;
    private final WorkflowStatusCatalog workflowStatusCatalog;

    public List<AvailableTransitionResponse.AvailableTransitionItem> buildFallbackItems(
            WorkflowContext ctx,
            Workflow workflow) {
        UUID current = ctx.getCurrentStatusId();
        List<Map<String, Object>> statuses = integrationClient.fetchIssueStatuses();
        if (statuses.isEmpty()) {
            log.warn("No issue statuses from catalog for fallback transitions on issue {}", ctx.getIssueId());
            return List.of();
        }

        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = workflowStatusCatalog.loadCatalog();
        List<AvailableTransitionResponse.AvailableTransitionItem> items = new ArrayList<>();

        for (Map<String, Object> status : statuses) {
            UUID statusId = parseUuid(status.get("id"));
            if (statusId == null || statusId.equals(current)) {
                continue;
            }
            String name = status.get("name") != null
                    ? status.get("name").toString()
                    : workflowStatusCatalog.resolveName(statusId, catalog);
            UUID syntheticId = UUID.nameUUIDFromBytes(
                    ("fallback-transition:" + workflow.getId() + ":" + statusId).getBytes(StandardCharsets.UTF_8));

            items.add(AvailableTransitionResponse.AvailableTransitionItem.builder()
                    .id(syntheticId)
                    .name("Move to " + name)
                    .description("Direct status change (workflow fallback)")
                    .toStatusId(statusId)
                    .toStatusName(name)
                    .hasScreen(false)
                    .build());
        }
        return items;
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
