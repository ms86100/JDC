package com.jira.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pushes workflow scheme project assignments to jira-workflow-service (canonical runtime store).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowSchemeBridgeService {

    private final WorkflowSchemeAdminProxyService workflowSchemeAdminProxyService;

    public void pushSchemeToProjects(String schemeId, List<String> projectIds) {
        if (schemeId == null || projectIds == null || projectIds.isEmpty()) {
            return;
        }
        try {
            workflowSchemeAdminProxyService.assignSchemeToProjects(schemeId, projectIds);
            log.info("Pushed workflow scheme {} to {} project(s)", schemeId, projectIds.size());
        } catch (Exception e) {
            log.warn("Workflow scheme bridge failed: {}", e.getMessage());
        }
    }
}
