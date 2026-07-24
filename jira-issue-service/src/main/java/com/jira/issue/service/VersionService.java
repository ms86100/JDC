package com.jira.issue.service;

import com.jira.issue.dto.CreateVersionRequest;
import com.jira.issue.dto.UpdateVersionRequest;
import com.jira.issue.dto.VersionResponse;
import com.jira.issue.entity.ProjectVersion;
import com.jira.issue.exception.DuplicateResourceException;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.ProjectVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionService {

    private final ProjectVersionRepository versionRepository;

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate;

    @Transactional
    public VersionResponse createVersion(CreateVersionRequest request, UUID currentUserId) {
        log.info("Creating version '{}' for project: {}", request.getName(), request.getProjectId());

        // Verify project exists via REST
        if (!projectExists(request.getProjectId())) {
            throw new ResourceNotFoundException("Project", "id", request.getProjectId());
        }

        // Check for duplicate name
        if (versionRepository.existsByProjectIdAndName(request.getProjectId(), request.getName())) {
            throw new DuplicateResourceException("Version with name '" + request.getName() + "' already exists in this project");
        }

        // Calculate sort order if not provided
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            List<ProjectVersion> existingVersions = versionRepository.findByProjectIdOrderBySortOrderAsc(request.getProjectId());
            sortOrder = existingVersions.isEmpty() ? 0 : existingVersions.get(existingVersions.size() - 1).getSortOrder() + 1;
        }

        ProjectVersion version = ProjectVersion.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .releaseDate(request.getReleaseDate())
                .sortOrder(sortOrder)
                .isReleased(request.getIsReleased() != null ? request.getIsReleased() : false)
                .isArchived(request.getIsArchived() != null ? request.getIsArchived() : false)
                .build();

        version = versionRepository.save(version);
        log.info("Version created successfully: {} ({})", version.getName(), version.getId());

        return mapToVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> getVersionsForProject(UUID projectId) {
        log.debug("Fetching versions for project: {}", projectId);

        List<ProjectVersion> versions = versionRepository.findByProjectIdOrderBySortOrderAsc(projectId);

        return versions.stream()
                .map(this::mapToVersionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VersionResponse getVersion(UUID versionId) {
        ProjectVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));
        return mapToVersionResponse(version);
    }

    @Transactional
    public VersionResponse updateVersion(UUID versionId, UpdateVersionRequest request) {
        log.info("Updating version: {}", versionId);

        ProjectVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        if (request.getName() != null) {
            // Check for duplicate name (excluding current version)
            versionRepository.findByProjectIdAndName(version.getProjectId(), request.getName())
                    .filter(v -> !v.getId().equals(versionId))
                    .ifPresent(v -> { throw new DuplicateResourceException("Version with name '" + request.getName() + "' already exists"); });
            version.setName(request.getName());
        }
        if (request.getDescription() != null) {
            version.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            version.setStartDate(request.getStartDate());
        }
        if (request.getReleaseDate() != null) {
            version.setReleaseDate(request.getReleaseDate());
        }
        if (request.getSortOrder() != null) {
            version.setSortOrder(request.getSortOrder());
        }
        if (request.getIsReleased() != null) {
            version.setIsReleased(request.getIsReleased());
        }
        if (request.getIsArchived() != null) {
            version.setIsArchived(request.getIsArchived());
        }

        version = versionRepository.save(version);
        log.info("Version updated successfully: {}", versionId);

        return mapToVersionResponse(version);
    }

    @Transactional
    public VersionResponse releaseVersion(UUID versionId, UUID releasedBy) {
        log.info("Releasing version: {} by user: {}", versionId, releasedBy);

        ProjectVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        version.setIsReleased(true);
        version.setReleasedBy(releasedBy);
        version.setReleasedAt(LocalDateTime.now());

        version = versionRepository.save(version);
        log.info("Version released successfully: {}", versionId);

        return mapToVersionResponse(version);
    }

    @Transactional
    public VersionResponse unreleaseVersion(UUID versionId) {
        log.info("Unreleasing version: {}", versionId);

        ProjectVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        version.setIsReleased(false);
        version.setReleasedBy(null);
        version.setReleasedAt(null);

        version = versionRepository.save(version);
        log.info("Version unreleased successfully: {}", versionId);

        return mapToVersionResponse(version);
    }

    @Transactional
    public VersionResponse archiveVersion(UUID versionId) {
        log.info("Archiving version: {}", versionId);

        ProjectVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        version.setIsArchived(true);
        version = versionRepository.save(version);

        log.info("Version archived successfully: {}", versionId);
        return mapToVersionResponse(version);
    }

    @Transactional
    public VersionResponse unarchiveVersion(UUID versionId) {
        log.info("Unarchiving version: {}", versionId);

        ProjectVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        version.setIsArchived(false);
        version = versionRepository.save(version);

        log.info("Version unarchived successfully: {}", versionId);
        return mapToVersionResponse(version);
    }

    @Transactional
    public void deleteVersion(UUID versionId) {
        log.info("Deleting version: {}", versionId);

        if (!versionRepository.existsById(versionId)) {
            throw new ResourceNotFoundException("Version", "id", versionId);
        }

        versionRepository.deleteById(versionId);
        log.info("Version deleted successfully: {}", versionId);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> getReleasedVersions(UUID projectId) {
        List<ProjectVersion> versions = versionRepository.findByProjectIdAndIsReleased(projectId, true);
        return versions.stream()
                .map(this::mapToVersionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> getUnreleasedVersions(UUID projectId) {
        List<ProjectVersion> versions = versionRepository.findByProjectIdAndIsReleased(projectId, false);
        return versions.stream()
                .filter(v -> !v.getIsArchived())
                .map(this::mapToVersionResponse)
                .collect(Collectors.toList());
    }

    private VersionResponse mapToVersionResponse(ProjectVersion version) {
        return VersionResponse.builder()
                .id(version.getId())
                .projectId(version.getProjectId())
                .name(version.getName())
                .description(version.getDescription())
                .startDate(version.getStartDate())
                .releaseDate(version.getReleaseDate())
                .isReleased(version.getIsReleased())
                .isArchived(version.getIsArchived())
                .sortOrder(version.getSortOrder())
                .releasedBy(version.getReleasedBy())
                .releasedAt(version.getReleasedAt())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .build();
    }

    private boolean projectExists(UUID projectId) {
        try {
            String url = String.format("%s/api/projects/%s", projectServiceUrl, projectId);
            restTemplate.getForEntity(url, Object.class);
            return true;
        } catch (Exception e) {
            log.warn("Project check failed for {}: {}", projectId, e.getMessage());
            return false;
        }
    }
}