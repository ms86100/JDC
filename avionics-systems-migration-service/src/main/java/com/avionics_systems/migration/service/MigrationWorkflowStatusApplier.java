package com.avionics_systems.migration.service;

import com.avionics_systems.migration.service.clients.WorkflowRuntimeClient;
import com.avionics_systems.migration.service.clients.dto.IssueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationWorkflowStatusApplier {

    private static final UUID MIGRATION_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final WorkflowStatusMappingService workflowStatusMappingService;
    private final WorkflowRuntimeClient workflowRuntimeClient;

    /**
     * Applies workflow scheme + status mapping after issue import so migrated issues open with correct status.
     */
    public void applyImportedStatus(UUID jobId, IssueResponse issue, String sourceStatus) {
        if (issue == null || sourceStatus == null || sourceStatus.isBlank()) {
            return;
        }
        UUID issueId = UUID.fromString(issue.getId());
        UUID projectId = UUID.fromString(issue.getProjectId());

        Map<String, Object> mappings = workflowStatusMappingService.getForJob(jobId);
        String targetStatusName = workflowStatusMappingService.resolveStatus(mappings, sourceStatus);
        UUID targetStatusId = workflowRuntimeClient.resolveStatusIdByName(targetStatusName);
        if (targetStatusId == null) {
            log.warn("No target status for imported issue {} (source='{}' -> '{}')", issue.getKey(), sourceStatus, targetStatusName);
            return;
        }

        if (issue.getStatus() != null && issue.getStatus().equalsIgnoreCase(targetStatusName)) {
            return;
        }

        UUID transitionId = workflowRuntimeClient.findTransitionIdToStatus(
                issueId, projectId, targetStatusId, MIGRATION_ACTOR_ID);
        try {
            if (transitionId != null) {
                workflowRuntimeClient.executeTransition(issueId, projectId, transitionId, MIGRATION_ACTOR_ID);
                log.info("Migrated issue {} transitioned to {} via workflow", issue.getKey(), targetStatusName);
            } else {
                workflowRuntimeClient.applyStatusInternal(issueId, projectId, targetStatusId);
                log.info("Migrated issue {} status set to {} (no legal transition; internal apply)", issue.getKey(), targetStatusName);
            }
        } catch (Exception e) {
            log.warn("Could not apply migrated status for {}: {}", issue.getKey(), e.getMessage());
        }
    }
}
