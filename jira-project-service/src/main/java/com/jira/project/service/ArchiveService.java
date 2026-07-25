package com.jira.project.service;

import com.jira.project.dto.ProjectResponse;
import com.jira.project.entity.Project;
import com.jira.project.exception.InvalidOperationException;
import com.jira.project.exception.ResourceNotFoundException;
import com.jira.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveService {

    private final ProjectRepository projectRepository;

    @Autowired
    private MessageSource messageSource;

    @Transactional
    public ProjectResponse archiveProject(UUID projectId) {
        log.info("Archiving project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (Boolean.TRUE.equals(project.getArchived())) {
            throw new InvalidOperationException(messageSource.getMessage("error.project.already.archived", null, "Project is already archived", Locale.ENGLISH));
        }

        project.setArchived(true);
        project.setArchivedAt(LocalDateTime.now());
        project = projectRepository.save(project);

        log.info("Project archived successfully: {}", projectId);
        return mapToProjectResponse(project);
    }

    @Transactional
    public ProjectResponse restoreProject(UUID projectId) {
        log.info("Restoring project: {}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (Boolean.FALSE.equals(project.getArchived())) {
            throw new InvalidOperationException(messageSource.getMessage("error.project.not.archived", null, "Project is not archived", Locale.ENGLISH));
        }

        project.setArchived(false);
        project.setArchivedAt(null);
        project = projectRepository.save(project);

        log.info("Project restored successfully: {}", projectId);
        return mapToProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getArchivedProjects() {
        log.debug("Fetching all archived projects");
        return projectRepository.findByArchivedTrue().stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getActiveProjects() {
        log.debug("Fetching all active (non-archived) projects");
        return projectRepository.findByArchivedFalse().stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectKey(project.getProjectKey())
                .name(project.getName())
                .description(project.getDescription())
                .leadUserId(project.getLeadUserId())
                .projectType(project.getProjectType())
                .templateId(project.getTemplateId())
                .category(project.getCategory())
                .avatarUrl(project.getAvatarUrl())
                .defaultAssigneeType(project.getDefaultAssigneeType())
                .allowIssueCreation(project.getAllowIssueCreation())
                .archived(project.getArchived())
                .archivedAt(project.getArchivedAt())
                .version(project.getVersion())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}