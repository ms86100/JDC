package com.avionics_systems.workflow.service;

import com.avionics_systems.workflow.entity.ProjectWorkflowScheme;
import com.avionics_systems.workflow.entity.WorkflowScheme;
import com.avionics_systems.workflow.entity.WorkflowSchemeMapping;
import com.avionics_systems.workflow.entity.WorkflowStatus;
import com.avionics_systems.workflow.exception.ResourceNotFoundException;
import com.avionics_systems.workflow.repository.ProjectWorkflowSchemeRepository;
import com.avionics_systems.workflow.repository.WorkflowSchemeMappingRepository;
import com.avionics_systems.workflow.repository.WorkflowSchemeRepository;
import com.avionics_systems.workflow.repository.WorkflowStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Canonical project ↔ workflow scheme assignment (jira_workflow) with sync to project-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectWorkflowSchemeBridgeService {

    private final ProjectWorkflowSchemeRepository projectWorkflowSchemeRepository;
    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final WorkflowSchemeMappingRepository workflowSchemeMappingRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final RestTemplate restTemplate;

    @Value("${avionics-systems.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Transactional
    public Map<String, Object> assignSchemeToProject(UUID projectId, UUID schemeId) {
        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", schemeId));

        // C4: Check if the project already has a different scheme assigned
        Optional<ProjectWorkflowScheme> existingLink = projectWorkflowSchemeRepository.findById(projectId);
        if (existingLink.isPresent() && !existingLink.get().getSchemeId().equals(schemeId)) {
            Map<String, Object> compatibility = validateSchemeCompatibility(existingLink.get().getSchemeId(), schemeId);
            @SuppressWarnings("unchecked")
            List<String> orphanedStatuses = (List<String>) compatibility.get("orphanedStatuses");
            if (orphanedStatuses != null && !orphanedStatuses.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("projectId", projectId.toString());
                response.put("schemeId", schemeId.toString());
                response.put("migrationRequired", true);
                response.put("orphanedStatuses", orphanedStatuses);
                response.put("message", "The new scheme is missing statuses used in the current scheme. "
                        + "Migrate issues in these statuses before switching.");
                log.warn("Scheme switch for project {} blocked: {} orphaned status(es)", projectId, orphanedStatuses.size());
                return response;
            }
        }

        ProjectWorkflowScheme link = existingLink
                .orElse(ProjectWorkflowScheme.builder().projectId(projectId).build());
        link.setSchemeId(schemeId);
        link.setUpdatedAt(LocalDateTime.now());
        projectWorkflowSchemeRepository.save(link);

        pushToProjectService(scheme, List.of(projectId.toString()));
        return Map.of("projectId", projectId.toString(), "schemeId", schemeId.toString());
    }

    /**
     * Compares the statuses available in the old scheme's workflows against those
     * in the new scheme's workflows, returning any "orphaned" status IDs that exist
     * in the old scheme but not in the new one.
     */
    private Map<String, Object> validateSchemeCompatibility(UUID oldSchemeId, UUID newSchemeId) {
        // Collect all status IDs across every workflow mapped in the old scheme
        Set<UUID> oldStatusIds = collectSchemeStatusIds(oldSchemeId);

        // Collect all status IDs across every workflow mapped in the new scheme
        Set<UUID> newStatusIds = collectSchemeStatusIds(newSchemeId);

        // Orphaned = statuses present in old but absent from new
        Set<UUID> orphaned = new HashSet<>(oldStatusIds);
        orphaned.removeAll(newStatusIds);

        Map<String, Object> result = new HashMap<>();
        result.put("orphanedStatuses", orphaned.stream().map(UUID::toString).collect(Collectors.toList()));
        result.put("compatible", orphaned.isEmpty());
        return result;
    }

    private Set<UUID> collectSchemeStatusIds(UUID schemeId) {
        List<WorkflowSchemeMapping> mappings = workflowSchemeMappingRepository.findBySchemeId(schemeId);
        Set<UUID> statusIds = new HashSet<>();
        for (WorkflowSchemeMapping m : mappings) {
            List<WorkflowStatus> statuses = workflowStatusRepository
                    .findByWorkflowIdOrderBySequenceAsc(m.getWorkflow().getId());
            for (WorkflowStatus s : statuses) {
                statusIds.add(s.getStatusId());
            }
        }
        return statusIds;
    }

    @Transactional
    public int assignSchemeToProjects(UUID schemeId, List<String> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return 0;
        }
        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", schemeId));

        int count = 0;
        for (String projectIdStr : projectIds) {
            try {
                UUID projectId = UUID.fromString(projectIdStr);
                assignSchemeToProject(projectId, schemeId);
                count++;
            } catch (IllegalArgumentException e) {
                log.warn("Skipping invalid project id: {}", projectIdStr);
            }
        }
        // Removed redundant pushToProjectService — each assignSchemeToProject() already pushes
        return count;
    }

    private void pushToProjectService(WorkflowScheme scheme, List<String> projectIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("schemeId", scheme.getId().toString());
        body.put("schemeName", scheme.getName());
        body.put("description", scheme.getDescription());
        body.put("projectIds", projectIds);

        String url = projectServiceUrl + "/api/projects/schemes/workflow/assign";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            log.info("Synced workflow scheme '{}' to project-service for {} project(s)", scheme.getName(), projectIds.size());
        } catch (Exception e) {
            log.warn("Project-service workflow scheme bridge failed: {}", e.getMessage());
        }
    }
}
