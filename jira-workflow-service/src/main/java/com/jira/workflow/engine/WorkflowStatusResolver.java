package com.jira.workflow.engine;

import com.jira.workflow.entity.WorkflowStatus;
import com.jira.workflow.repository.WorkflowStatusRepository;
import com.jira.workflow.repository.WorkflowTransitionRepository;
import com.jira.workflow.service.WorkflowStatusCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
