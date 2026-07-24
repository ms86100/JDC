package com.jira.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jira.admin.entity.ProjectEntity;
import com.jira.admin.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Mirrors projects from jira-project-service into jira_admin.projects for scheme assignment UI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectCatalogSyncService {

    private final ProjectRepository projectRepository;
    private final RestTemplate restTemplate;

    @Value("${jira.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Transactional
    public int syncFromProjectService() {
        String url = projectServiceUrl + "/api/projects/all";
        JsonNode[] remoteProjects;
        try {
            remoteProjects = restTemplate.getForObject(url, JsonNode[].class);
        } catch (Exception e) {
            log.warn("Could not sync projects from {}: {}", url, e.getMessage());
            return 0;
        }
        if (remoteProjects == null) {
            return 0;
        }

        int synced = 0;
        for (JsonNode node : remoteProjects) {
            String id = text(node, "id");
            String key = text(node, "projectKey");
            String name = text(node, "name");
            if (id == null || key == null || name == null) {
                continue;
            }

            Optional<ProjectEntity> existing = projectRepository.findByProjectKey(key);
            if (existing.isPresent()) {
                ProjectEntity p = existing.get();
                p.setName(name);
                if (node.hasNonNull("description")) {
                    p.setDescription(node.get("description").asText(""));
                }
                if (node.has("archived") && node.get("archived").asBoolean()) {
                    p.setStatus(ProjectEntity.ProjectStatus.ARCHIVED);
                } else {
                    p.setStatus(ProjectEntity.ProjectStatus.ACTIVE);
                }
                projectRepository.save(p);
            } else {
                projectRepository.save(ProjectEntity.builder()
                        .id(id)
                        .projectKey(key)
                        .name(name)
                        .description(text(node, "description"))
                        .status(node.has("archived") && node.get("archived").asBoolean()
                                ? ProjectEntity.ProjectStatus.ARCHIVED
                                : ProjectEntity.ProjectStatus.ACTIVE)
                        .build());
            }
            synced++;
        }
        log.info("Synced {} project(s) from project-service", synced);
        return synced;
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}
