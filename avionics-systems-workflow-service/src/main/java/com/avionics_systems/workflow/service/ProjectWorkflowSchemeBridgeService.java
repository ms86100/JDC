package com.avionics_systems.workflow.service;

import com.avionics_systems.workflow.entity.ProjectWorkflowScheme;
import com.avionics_systems.workflow.entity.WorkflowScheme;
import com.avionics_systems.workflow.exception.ResourceNotFoundException;
import com.avionics_systems.workflow.repository.ProjectWorkflowSchemeRepository;
import com.avionics_systems.workflow.repository.WorkflowSchemeRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical project ↔ workflow scheme assignment (jira_workflow) with sync to project-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectWorkflowSchemeBridgeService {

    private final ProjectWorkflowSchemeRepository projectWorkflowSchemeRepository;
    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final RestTemplate restTemplate;

    @Value("${avionics-systems.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Transactional
    public Map<String, Object> assignSchemeToProject(UUID projectId, UUID schemeId) {
        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowScheme", "id", schemeId));

        ProjectWorkflowScheme link = projectWorkflowSchemeRepository.findById(projectId)
                .orElse(ProjectWorkflowScheme.builder().projectId(projectId).build());
        link.setSchemeId(schemeId);
        link.setUpdatedAt(LocalDateTime.now());
        projectWorkflowSchemeRepository.save(link);

        pushToProjectService(scheme, List.of(projectId.toString()));
        return Map.of("projectId", projectId.toString(), "schemeId", schemeId.toString());
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
        pushToProjectService(scheme, projectIds);
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
