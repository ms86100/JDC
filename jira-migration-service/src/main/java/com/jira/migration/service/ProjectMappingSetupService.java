package com.jira.migration.service;

import com.jira.migration.entity.MigrationJob;
import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.repository.ProjectMappingRepository;
import com.jira.migration.service.clients.ProjectServiceClient;
import com.jira.migration.service.clients.dto.ProjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Ensures {@link ProjectMapping} rows exist so issue import can resolve target project IDs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMappingSetupService {

    private final MigrationJobRepository migrationJobRepository;
    private final ProjectMappingRepository projectMappingRepository;
    private final ProjectServiceClient projectServiceClient;

    @Transactional
    public void ensureProjectMappings(UUID jobId, UUID targetProjectId, List<Map<String, String>> rows) {
        if (targetProjectId == null) {
            return;
        }

        MigrationJob job = migrationJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        ProjectResponse targetProject;
        try {
            targetProject = projectServiceClient.getProject(targetProjectId.toString());
        } catch (Exception e) {
            log.warn("Could not load target project {} for job {}: {}", targetProjectId, jobId, e.getMessage());
            return;
        }

        String targetKey = targetProject.getKey();
        UUID targetUuid = parseUuid(targetProject.getId());

        Set<String> sourceKeys = new LinkedHashSet<>();
        if (targetKey != null) {
            sourceKeys.add(targetKey);
        }
        for (Map<String, String> row : rows) {
            String pk = row.get("project_key");
            if (pk != null && !pk.isBlank()) {
                sourceKeys.add(pk.trim().toUpperCase(Locale.ROOT));
            }
        }

        for (String sourceKey : sourceKeys) {
            if (sourceKey == null || sourceKey.isBlank()) {
                continue;
            }
            Optional<ProjectMapping> existing =
                    projectMappingRepository.findByJobIdAndSourceKey(jobId, sourceKey);
            if (existing.isPresent()) {
                continue;
            }
            projectMappingRepository.save(ProjectMapping.builder()
                    .jobId(jobId)
                    .sourceKey(sourceKey)
                    .targetKey(targetKey)
                    .targetId(targetUuid != null ? targetUuid : targetProjectId)
                    .build());
            log.info("Created project mapping for job {}: {} -> {}", jobId, sourceKey, targetKey);
        }
    }

    private UUID parseUuid(String id) {
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
