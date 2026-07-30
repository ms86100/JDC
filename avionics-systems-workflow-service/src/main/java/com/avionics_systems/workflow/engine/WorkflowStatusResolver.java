package com.avionics_systems.workflow.engine;

import com.avionics_systems.workflow.entity.WorkflowStatus;
import com.avionics_systems.workflow.repository.WorkflowStatusRepository;
import com.avionics_systems.workflow.repository.WorkflowTransitionRepository;
import com.avionics_systems.workflow.service.WorkflowStatusCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maps issue-service status IDs to workflow transition {@code from_status_id} values.
 */
@Component
@RequiredArgsConstructor
public class WorkflowStatusResolver {

    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowStatusCatalog workflowStatusCatalog;

    @Value("${app.workflow.status-resolver.strip-suffixes:(legacy),(new)}")
    private String stripSuffixesStr;

    public UUID resolveForTransitions(UUID workflowId, UUID issueStatusId, Map<String, Object> issueData) {
        if (issueStatusId == null || workflowId == null) {
            return issueStatusId;
        }

        if (hasOutgoing(workflowId, issueStatusId)) {
            return issueStatusId;
        }

        List<WorkflowStatus> workflowStatuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId);
        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = workflowStatusCatalog.loadCatalog();
        String issueStatusName = resolveIssueStatusName(issueStatusId, issueData, catalog);

        for (WorkflowStatus ws : workflowStatuses) {
            if (issueStatusId.equals(ws.getStatusId()) && hasOutgoing(workflowId, ws.getStatusId())) {
                return ws.getStatusId();
            }
            String wsName = workflowStatusCatalog.resolveName(ws.getStatusId(), catalog);
            if (issueStatusName != null
                    && issueStatusName.equalsIgnoreCase(wsName)
                    && hasOutgoing(workflowId, ws.getStatusId())) {
                return ws.getStatusId();
            }
        }

        for (WorkflowStatus ws : workflowStatuses) {
            if (hasOutgoing(workflowId, ws.getStatusId())) {
                return ws.getStatusId();
            }
        }

        return issueStatusId;
    }

    private boolean hasOutgoing(UUID workflowId, UUID fromStatusId) {
        return !workflowTransitionRepository.findByWorkflowIdAndFromStatusId(workflowId, fromStatusId).isEmpty();
    }

    private String resolveIssueStatusName(
            UUID issueStatusId,
            Map<String, Object> issueData,
            Map<String, WorkflowStatusCatalog.StatusMeta> catalog) {
        Object rawName = issueData != null ? issueData.get("status") : null;
        if (rawName != null && !rawName.toString().isBlank()) {
            return rawName.toString();
        }
        return workflowStatusCatalog.resolveName(issueStatusId, catalog);
    }

    /**
     * Whether the issue's current status satisfies a transition's {@code from_status_id}.
     * Matches exact IDs, equivalent display names (e.g. legacy vs canonical Backlog),
     * or the same remapping used when listing available transitions.
     */
    public boolean statusesMatchForTransition(
            UUID workflowId,
            UUID issueStatusId,
            UUID requiredFromStatusId,
            Map<String, Object> issueData) {
        if (requiredFromStatusId == null || issueStatusId == null) {
            return false;
        }
        if (requiredFromStatusId.equals(issueStatusId)) {
            return true;
        }

        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = workflowStatusCatalog.loadCatalog();
        String issueName = normalizeStatusName(resolveIssueStatusName(issueStatusId, issueData, catalog));
        String requiredName = normalizeStatusName(workflowStatusCatalog.resolveName(requiredFromStatusId, catalog));
        if (!issueName.isEmpty() && !requiredName.isEmpty() && issueName.equals(requiredName)) {
            return true;
        }

        UUID resolvedForListing = resolveForTransitions(workflowId, issueStatusId, issueData);
        return requiredFromStatusId.equals(resolvedForListing);
    }

    private String normalizeStatusName(String name) {
        if (name == null) {
            return "";
        }
        String result = name.toLowerCase();
        if (stripSuffixesStr != null && !stripSuffixesStr.isBlank()) {
            for (String suffix : stripSuffixesStr.split(",")) {
                result = result.replace(suffix.trim().toLowerCase(), "");
            }
        }
        return result.replaceAll("[\\s_\\-()]+", "");
    }
}
