package com.avionics_systems.project.service;

import com.avionics_systems.project.dto.BulkProjectResponse;
import com.avionics_systems.project.entity.Project;
import com.avionics_systems.project.exception.ResourceNotFoundException;
import com.avionics_systems.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkProjectService {

    private final ProjectRepository projectRepository;
    private final ArchiveService archiveService;
    private final ProjectService projectService;

    @Autowired
    private MessageSource messageSource;

    @Transactional
    public BulkProjectResponse bulkArchive(List<UUID> projectIds) {
        log.info("Bulk archiving {} projects", projectIds.size());

        List<BulkProjectResponse.BulkOperationResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (UUID projectId : projectIds) {
            try {
                archiveService.archiveProject(projectId);
                results.add(BulkProjectResponse.BulkOperationResult.builder()
                        .projectId(projectId.toString())
                        .success(true)
                        .message(messageSource.getMessage("bulk.project.archived.success", null, "Project archived successfully", Locale.ENGLISH))
                        .build());
                successCount++;
            } catch (Exception e) {
                log.error("Failed to archive project {}: {}", projectId, e.getMessage());
                results.add(BulkProjectResponse.BulkOperationResult.builder()
                        .projectId(projectId.toString())
                        .success(false)
                        .message(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        return BulkProjectResponse.builder()
                .totalRequested(projectIds.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    @Transactional
    public BulkProjectResponse bulkRestore(List<UUID> projectIds) {
        log.info("Bulk restoring {} projects", projectIds.size());

        List<BulkProjectResponse.BulkOperationResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (UUID projectId : projectIds) {
            try {
                archiveService.restoreProject(projectId);
                results.add(BulkProjectResponse.BulkOperationResult.builder()
                        .projectId(projectId.toString())
                        .success(true)
                        .message(messageSource.getMessage("bulk.project.restored.success", null, "Project restored successfully", Locale.ENGLISH))
                        .build());
                successCount++;
            } catch (Exception e) {
                log.error("Failed to restore project {}: {}", projectId, e.getMessage());
                results.add(BulkProjectResponse.BulkOperationResult.builder()
                        .projectId(projectId.toString())
                        .success(false)
                        .message(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        return BulkProjectResponse.builder()
                .totalRequested(projectIds.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    @Transactional
    public BulkProjectResponse bulkDelete(List<UUID> projectIds) {
        log.info("Bulk deleting {} projects", projectIds.size());

        List<BulkProjectResponse.BulkOperationResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (UUID projectId : projectIds) {
            try {
                projectService.deleteProject(projectId);
                results.add(BulkProjectResponse.BulkOperationResult.builder()
                        .projectId(projectId.toString())
                        .success(true)
                        .message(messageSource.getMessage("bulk.project.deleted.success", null, "Project deleted successfully", Locale.ENGLISH))
                        .build());
                successCount++;
            } catch (Exception e) {
                log.error("Failed to delete project {}: {}", projectId, e.getMessage());
                results.add(BulkProjectResponse.BulkOperationResult.builder()
                        .projectId(projectId.toString())
                        .success(false)
                        .message(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        return BulkProjectResponse.builder()
                .totalRequested(projectIds.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    @Transactional
    public BulkProjectResponse bulkArchiveByCategory(String category) {
        log.info("Bulk archiving projects in category: {}", category);

        List<Project> projects = projectRepository.findByArchivedFalse().stream()
                .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                .collect(Collectors.toList());

        if (projects.isEmpty()) {
            return BulkProjectResponse.builder()
                    .totalRequested(0)
                    .successCount(0)
                    .failureCount(0)
                    .results(List.of())
                    .build();
        }

        List<UUID> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        return bulkArchive(projectIds);
    }

    @Transactional
    public BulkProjectResponse bulkArchiveByType(String projectType) {
        log.info("Bulk archiving projects of type: {}", projectType);

        List<Project> projects = projectRepository.findByArchivedFalse().stream()
                .filter(p -> projectType.equalsIgnoreCase(p.getProjectType()))
                .collect(Collectors.toList());

        if (projects.isEmpty()) {
            return BulkProjectResponse.builder()
                    .totalRequested(0)
                    .successCount(0)
                    .failureCount(0)
                    .results(List.of())
                    .build();
        }

        List<UUID> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        return bulkArchive(projectIds);
    }
}