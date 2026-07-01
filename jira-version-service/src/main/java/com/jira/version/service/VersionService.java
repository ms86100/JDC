package com.jira.version.service;

import com.jira.version.dto.*;
import com.jira.version.entity.*;
import com.jira.version.exception.*;
import com.jira.version.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionService {

    private final ProjectVersionRepository versionRepository;
    private final IssueFixVersionRepository fixVersionRepository;
    private final IssueAffectsVersionRepository affectsVersionRepository;
    private final VersionReleaseNoteRepository releaseNoteRepository;
    private final VersionAuditLogRepository auditLogRepository;
    private final VersionMetricSnapshotRepository metricSnapshotRepository;
    private final VersionDeploymentRepository deploymentRepository;
    private final VersionBuildReferenceRepository buildReferenceRepository;
    private final ReleaseTrainRepository releaseTrainRepository;
    private final ReleaseTrainVersionRepository trainVersionRepository;

    // ========== VERSION CRUD ==========

    @Transactional(readOnly = true)
    public List<VersionResponse> getVersionsByProject(UUID projectId, boolean includeArchived) {
        List<ProjectVersion> versions;
        if (includeArchived) {
            versions = versionRepository.findByProjectIdAndDeletedFalseOrderBySequenceAsc(projectId);
        } else {
            versions = versionRepository.findActiveByProjectId(projectId);
        }
        return versions.stream()
            .map(this::toVersionResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VersionResponse getVersionById(UUID versionId) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));
        return toVersionResponse(version);
    }

    @Transactional
    public VersionResponse createVersion(CreateVersionRequest request) {
        // Check for duplicate name
        if (versionRepository.existsByProjectIdAndNameAndIdNot(request.getProjectId(), request.getName(), null)) {
            throw new DuplicateResourceException("Version with name '" + request.getName() + "' already exists in this project");
        }

        // Get next sequence
        int nextSequence = (int) versionRepository.countByProjectId(request.getProjectId());

        ProjectVersion version = ProjectVersion.builder()
            .projectId(request.getProjectId())
            .name(request.getName())
            .description(request.getDescription())
            .startDate(request.getStartDate())
            .releaseDate(request.getReleaseDate())
            .semanticVersion(request.getSemanticVersion())
            .buildNumber(request.getBuildNumber())
            .branchName(request.getBranchName())
            .releaseTrain(request.getReleaseTrain())
            .color(request.getColor())
            .sequence(nextSequence)
            .released(false)
            .archived(false)
            .deleted(false)
            .releaseStatus("UNRELEASED")
            .deploymentStatus("PLANNED")
            .build();

        version = versionRepository.save(version);

        // Audit log
        createAuditLog(version.getId(), "CREATED", null, null, "Version created");

        log.info("Created version: {} for project: {}", version.getName(), version.getProjectId());
        return toVersionResponse(version);
    }

    @Transactional
    public VersionResponse updateVersion(UUID versionId, UpdateVersionRequest request, UUID userId) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        if (version.getReleased()) {
            throw new InvalidOperationException("Cannot modify a released version");
        }

        // Check for duplicate name
        if (request.getName() != null && !request.getName().equals(version.getName())) {
            if (versionRepository.existsByProjectIdAndNameAndIdNot(version.getProjectId(), request.getName(), versionId)) {
                throw new DuplicateResourceException("Version with name '" + request.getName() + "' already exists in this project");
            }
        }

        // Track changes for audit
        if (request.getName() != null && !request.getName().equals(version.getName())) {
            createAuditLog(versionId, "UPDATED", "name", version.getName(), request.getName(), userId);
        }
        if (request.getDescription() != null && !request.getDescription().equals(version.getDescription())) {
            createAuditLog(versionId, "UPDATED", "description", version.getDescription(), request.getDescription(), userId);
        }

        // Update fields
        if (request.getName() != null) version.setName(request.getName());
        if (request.getDescription() != null) version.setDescription(request.getDescription());
        if (request.getStartDate() != null) version.setStartDate(request.getStartDate());
        if (request.getReleaseDate() != null) version.setReleaseDate(request.getReleaseDate());
        if (request.getSemanticVersion() != null) version.setSemanticVersion(request.getSemanticVersion());
        if (request.getBuildNumber() != null) version.setBuildNumber(request.getBuildNumber());
        if (request.getBranchName() != null) version.setBranchName(request.getBranchName());
        if (request.getReleaseTrain() != null) version.setReleaseTrain(request.getReleaseTrain());
        if (request.getColor() != null) version.setColor(request.getColor());
        if (request.getSequence() != null) version.setSequence(request.getSequence());

        version = versionRepository.save(version);
        log.info("Updated version: {}", versionId);

        return toVersionResponse(version);
    }

    @Transactional
    public void deleteVersion(UUID versionId) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        version.setDeleted(true);
        versionRepository.save(version);

        createAuditLog(versionId, "DELETED", null, null, "Version deleted");
        log.info("Deleted version: {}", versionId);
    }

    // ========== RELEASE OPERATIONS ==========

    @Transactional
    public VersionResponse releaseVersion(UUID versionId, ReleaseVersionRequest request) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        if (version.getReleased()) {
            throw new InvalidOperationException("Version is already released");
        }

        version.setReleased(true);
        version.setReleasedBy(request.getReleasedBy());
        version.setActualReleaseDate(request.getActualReleaseDate() != null ? request.getActualReleaseDate() : LocalDateTime.now());
        version.setReleaseStatus("RELEASED");
        version.setDeploymentStatus("DEPLOYED");

        if (request.getReleaseNotesUrl() != null) {
            version.setReleaseNotesUrl(request.getReleaseNotesUrl());
        }

        version = versionRepository.save(version);

        createAuditLog(versionId, "RELEASED", null, null, "Version released", request.getReleasedBy());

        // Generate release notes if requested
        if (Boolean.TRUE.equals(request.getGenerateReleaseNotes())) {
            generateReleaseNotes(versionId, request.getReleasedBy());
        }

        log.info("Released version: {}", versionId);
        return toVersionResponse(version);
    }

    @Transactional
    public VersionResponse archiveVersion(UUID versionId, UUID userId) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        version.setArchived(true);
        version.setArchivedBy(userId);
        versionRepository.save(version);

        createAuditLog(versionId, "ARCHIVED", null, null, "Version archived", userId);
        log.info("Archived version: {}", versionId);

        return toVersionResponse(version);
    }

    @Transactional
    public VersionResponse unarchiveVersion(UUID versionId, UUID userId) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        version.setArchived(false);
        version.setArchivedBy(null);
        versionRepository.save(version);

        createAuditLog(versionId, "UNARCHIVED", null, null, "Version unarchived", userId);
        log.info("Unarchived version: {}", versionId);

        return toVersionResponse(version);
    }

    @Transactional
    public VersionResponse restoreVersion(UUID versionId) {
        ProjectVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        version.setDeleted(false);
        version = versionRepository.save(version);

        createAuditLog(versionId, "RESTORED", null, null, "Version restored");
        log.info("Restored version: {}", versionId);

        return toVersionResponse(version);
    }

    // ========== VERSION ISSUE LINKING ==========

    @Transactional
    public void assignFixVersion(UUID issueId, UUID versionId, UUID userId) {
        if (fixVersionRepository.existsByIssueIdAndVersionId(issueId, versionId)) {
            return; // Already linked
        }

        IssueFixVersion fixVersion = IssueFixVersion.builder()
            .issueId(issueId)
            .versionId(versionId)
            .createdBy(userId)
            .build();

        fixVersionRepository.save(fixVersion);
        createAuditLog(versionId, "ISSUE_ADDED", "issueId", null, "Issue " + issueId + " added to fix version", userId);

        log.info("Assigned fix version {} to issue {}", versionId, issueId);
    }

    @Transactional
    public void removeFixVersion(UUID issueId, UUID versionId) {
        List<IssueFixVersion> links = fixVersionRepository.findByIssueId(issueId);
        links.stream()
            .filter(l -> l.getVersionId().equals(versionId))
            .findFirst()
            .ifPresent(fixVersionRepository::delete);

        createAuditLog(versionId, "ISSUE_REMOVED", "issueId", issueId.toString(), "Issue removed from fix version");
    }

    @Transactional
    public void assignAffectsVersion(UUID issueId, UUID versionId, UUID userId) {
        if (affectsVersionRepository.existsByIssueIdAndVersionId(issueId, versionId)) {
            return;
        }

        IssueAffectsVersion affectsVersion = IssueAffectsVersion.builder()
            .issueId(issueId)
            .versionId(versionId)
            .createdBy(userId)
            .build();

        affectsVersionRepository.save(affectsVersion);
        createAuditLog(versionId, "ISSUE_ADDED", "issueId", null, "Issue " + issueId + " added to affects version", userId);

        log.info("Assigned affects version {} to issue {}", versionId, issueId);
    }

    @Transactional
    public void removeAffectsVersion(UUID issueId, UUID versionId) {
        List<IssueAffectsVersion> links = affectsVersionRepository.findByIssueId(issueId);
        links.stream()
            .filter(l -> l.getVersionId().equals(versionId))
            .findFirst()
            .ifPresent(affectsVersionRepository::delete);

        createAuditLog(versionId, "ISSUE_REMOVED", "issueId", issueId.toString(), "Issue removed from affects version");
    }

    // ========== BULK OPERATIONS ==========

    @Transactional
    public int bulkAssignFixVersion(List<UUID> issueIds, UUID versionId, UUID userId) {
        int count = 0;
        for (UUID issueId : issueIds) {
            if (!fixVersionRepository.existsByIssueIdAndVersionId(issueId, versionId)) {
                IssueFixVersion fixVersion = IssueFixVersion.builder()
                    .issueId(issueId)
                    .versionId(versionId)
                    .createdBy(userId)
                    .build();
                fixVersionRepository.save(fixVersion);
                count++;
            }
        }
        createAuditLog(versionId, "BULK_ASSIGN", "issueCount", null, count + " issues assigned to version", userId);
        log.info("Bulk assigned {} issues to fix version {}", count, versionId);
        return count;
    }

    @Transactional
    public int bulkMoveFixVersion(List<UUID> issueIds, UUID sourceVersionId, UUID targetVersionId, UUID userId) {
        int[] movedCount = {0};

        for (UUID issueId : issueIds) {
            // Remove from source
            List<IssueFixVersion> links = fixVersionRepository.findByIssueId(issueId);
            links.stream()
                .filter(l -> l.getVersionId().equals(sourceVersionId))
                .findFirst()
                .ifPresent(link -> {
                    fixVersionRepository.delete(link);
                    movedCount[0]++;
                });

            // Add to target
            if (!fixVersionRepository.existsByIssueIdAndVersionId(issueId, targetVersionId)) {
                IssueFixVersion newLink = IssueFixVersion.builder()
                    .issueId(issueId)
                    .versionId(targetVersionId)
                    .createdBy(userId)
                    .build();
                fixVersionRepository.save(newLink);
            }
        }

        createAuditLog(targetVersionId, "BULK_MOVE", "issueCount", null, movedCount[0] + " issues moved to version", userId);
        log.info("Bulk moved {} issues from version {} to {}", movedCount[0], sourceVersionId, targetVersionId);
        return movedCount[0];
    }

    // ========== MERGE OPERATIONS ==========

    @Transactional
    public VersionResponse mergeVersions(MergeVersionsRequest request) {
        ProjectVersion sourceVersion = versionRepository.findByIdAndDeletedFalse(request.getSourceVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", request.getSourceVersionId()));
        ProjectVersion targetVersion = versionRepository.findByIdAndDeletedFalse(request.getTargetVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", request.getTargetVersionId()));

        if (!sourceVersion.getProjectId().equals(targetVersion.getProjectId())) {
            throw new InvalidOperationException("Cannot merge versions from different projects");
        }

        // Move specified issues
        if (request.getIssueIdsToMove() != null && !request.getIssueIdsToMove().isEmpty()) {
            bulkMoveFixVersion(request.getIssueIdsToMove(), sourceVersion.getId(), targetVersion.getId(), null);
        }

        // Move remaining issues
        List<IssueFixVersion> remainingFixVersions = fixVersionRepository.findByVersionId(sourceVersion.getId());
        for (IssueFixVersion fixVersion : remainingFixVersions) {
            if (!fixVersionRepository.existsByIssueIdAndVersionId(fixVersion.getIssueId(), targetVersion.getId())) {
                fixVersion.setVersionId(targetVersion.getId());
                fixVersionRepository.save(fixVersion);
            } else {
                fixVersionRepository.delete(fixVersion);
            }
        }

        // Move affects versions
        List<IssueAffectsVersion> remainingAffectsVersions = affectsVersionRepository.findByVersionId(sourceVersion.getId());
        for (IssueAffectsVersion affectsVersion : remainingAffectsVersions) {
            if (!affectsVersionRepository.existsByIssueIdAndVersionId(affectsVersion.getIssueId(), targetVersion.getId())) {
                affectsVersion.setVersionId(targetVersion.getId());
                affectsVersionRepository.save(affectsVersion);
            } else {
                affectsVersionRepository.delete(affectsVersion);
            }
        }

        // Delete source version
        sourceVersion.setDeleted(true);
        versionRepository.save(sourceVersion);

        createAuditLog(targetVersion.getId(), "MERGED", "sourceVersion", sourceVersion.getName(), "Merged from " + sourceVersion.getName());

        log.info("Merged version {} into {}", sourceVersion.getId(), targetVersion.getId());
        return toVersionResponse(targetVersion);
    }

    // ========== RELEASE NOTES ==========

    @Transactional
    public VersionReleaseNoteResponse generateReleaseNotes(UUID versionId, UUID userId) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        // Get all issues fixed in this version
        List<IssueFixVersion> fixVersions = fixVersionRepository.findByVersionId(versionId);

        // Generate release notes content (simplified)
        StringBuilder content = new StringBuilder();
        content.append("# Release Notes - ").append(version.getName()).append("\n\n");
        content.append("Release Date: ").append(version.getActualReleaseDate()).append("\n\n");

        if (!fixVersions.isEmpty()) {
            content.append("## Fixed Issues\n\n");
            for (IssueFixVersion fv : fixVersions) {
                content.append("- ").append(fv.getIssueId()).append("\n");
            }
        }

        content.append("\n---\nGenerated by Jira Platform on ").append(LocalDateTime.now());

        // Save or update release notes
        VersionReleaseNote releaseNote = releaseNoteRepository.findByVersionId(versionId)
            .orElse(VersionReleaseNote.builder()
                .versionId(versionId)
                .generatedBy(userId)
                .build());

        releaseNote.setContent(content.toString());
        releaseNote.setGeneratedAt(LocalDateTime.now());
        releaseNote.setGeneratedBy(userId);

        releaseNote = releaseNoteRepository.save(releaseNote);

        // Update version
        version.setReleaseNotesGenerated(true);
        version.setReleaseNotesUrl("/versions/" + versionId + "/release-notes");
        versionRepository.save(version);

        createAuditLog(versionId, "RELEASE_NOTES_GENERATED", null, null, "Release notes generated");

        return toReleaseNoteResponse(releaseNote);
    }

    // ========== METRICS ==========

    @Transactional
    public List<VersionMetricsResponse> getVersionMetrics(UUID versionId) {
        List<VersionMetricSnapshot> snapshots = metricSnapshotRepository.findByVersionIdOrderBySnapshotDateAsc(versionId);
        return snapshots.stream()
            .map(this::toMetricsResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public VersionMetricsResponse recordMetricsSnapshot(UUID versionId) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        // Get issue counts
        long totalFixVersions = fixVersionRepository.countByVersionId(versionId);
        // Note: In a real implementation, you'd query issue status to get open/closed counts

        VersionMetricSnapshot snapshot = VersionMetricSnapshot.builder()
            .versionId(versionId)
            .snapshotDate(java.time.LocalDate.now())
            .totalIssues((int) totalFixVersions)
            .openIssues((int) totalFixVersions) // Placeholder
            .closedIssues(0) // Placeholder
            .resolvedIssues(0)
            .progressPercentage(java.math.BigDecimal.ZERO)
            .build();

        snapshot = metricSnapshotRepository.save(snapshot);
        return toMetricsResponse(snapshot);
    }

    // ========== DEPLOYMENTS ==========

    @Transactional
    public List<VersionDeploymentResponse> getVersionDeployments(UUID versionId) {
        List<VersionDeployment> deployments = deploymentRepository.findByVersionId(versionId);
        return deployments.stream()
            .map(this::toDeploymentResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public VersionDeploymentResponse addDeployment(UUID versionId, VersionDeployment deployment) {
        ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version", "id", versionId));

        deployment.setVersionId(versionId);
        deployment = deploymentRepository.save(deployment);

        // Update deployment status
        version.setDeploymentStatus(deployment.getStatus());
        versionRepository.save(version);

        createAuditLog(versionId, "DEPLOYMENT_ADDED", "environment", null, "Deployment to " + deployment.getEnvironment() + " recorded");

        return toDeploymentResponse(deployment);
    }

    // ========== BUILD REFERENCES ==========

    @Transactional
    public List<VersionBuildReferenceResponse> getVersionBuilds(UUID versionId) {
        List<VersionBuildReference> builds = buildReferenceRepository.findByVersionIdOrderByCreatedAtDesc(versionId);
        return builds.stream()
            .map(this::toBuildReferenceResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public VersionBuildReferenceResponse addBuildReference(UUID versionId, VersionBuildReference build) {
        build.setVersionId(versionId);
        build = buildReferenceRepository.save(build);

        if (build.getBuildNumber() != null) {
            ProjectVersion version = versionRepository.findByIdAndDeletedFalse(versionId).orElse(null);
            if (version != null) {
                version.setBuildNumber(build.getBuildNumber());
                versionRepository.save(version);
            }
        }

        createAuditLog(versionId, "BUILD_REFERENCE_ADDED", "buildNumber", null, "Build " + build.getBuildNumber() + " linked");

        return toBuildReferenceResponse(build);
    }

    // ========== RELEASE TRAINS ==========

    @Transactional
    public List<ReleaseTrain> getReleaseTrains() {
        return releaseTrainRepository.findActiveOrderByStartDate();
    }

    @Transactional
    public ReleaseTrain createReleaseTrain(ReleaseTrain train) {
        train = releaseTrainRepository.save(train);
        createAuditLog(null, "RELEASE_TRAIN_CREATED", "name", null, "Release train " + train.getName() + " created");
        return train;
    }

    @Transactional
    public void addVersionToReleaseTrain(UUID trainId, UUID versionId) {
        if (trainVersionRepository.existsByTrainIdAndVersionId(trainId, versionId)) {
            return;
        }

        int sequence = trainVersionRepository.findByTrainIdOrderBySequenceAsc(trainId).size();

        ReleaseTrainVersion trainVersion = ReleaseTrainVersion.builder()
            .trainId(trainId)
            .versionId(versionId)
            .sequence(sequence)
            .build();

        trainVersionRepository.save(trainVersion);
        log.info("Added version {} to release train {}", versionId, trainId);
    }

    @Transactional
    public void removeVersionFromReleaseTrain(UUID trainId, UUID versionId) {
        trainVersionRepository.deleteByTrainIdAndVersionId(trainId, versionId);
        log.info("Removed version {} from release train {}", versionId, trainId);
    }

    // ========== AUDIT ==========

    @Transactional(readOnly = true)
    public List<VersionAuditLog> getVersionAuditLogs(UUID versionId) {
        return auditLogRepository.findByVersionIdOrderByCreatedAtDesc(versionId);
    }

    private void createAuditLog(UUID versionId, String action, String fieldName, String oldValue, String newValue, UUID userId) {
        if (versionId == null) return;

        VersionAuditLog auditLog = VersionAuditLog.builder()
            .versionId(versionId)
            .action(action)
            .fieldName(fieldName)
            .oldValue(oldValue)
            .newValue(newValue)
            .userId(userId)
            .build();

        auditLogRepository.save(auditLog);
    }

    private void createAuditLog(UUID versionId, String action, String fieldName, String oldValue, String newValue) {
        createAuditLog(versionId, action, fieldName, oldValue, newValue, null);
    }

    // ========== HELPERS ==========

    private VersionResponse toVersionResponse(ProjectVersion version) {
        long fixVersionCount = fixVersionRepository.countByVersionId(version.getId());
        long affectsVersionCount = affectsVersionRepository.countByVersionId(version.getId());

        return VersionResponse.builder()
            .id(version.getId())
            .projectId(version.getProjectId())
            .name(version.getName())
            .description(version.getDescription())
            .released(version.getReleased())
            .archived(version.getArchived())
            .sequence(version.getSequence())
            .startDate(version.getStartDate())
            .releaseDate(version.getReleaseDate())
            .actualReleaseDate(version.getActualReleaseDate())
            .semanticVersion(version.getSemanticVersion())
            .buildNumber(version.getBuildNumber())
            .branchName(version.getBranchName())
            .releaseTrain(version.getReleaseTrain())
            .deploymentStatus(version.getDeploymentStatus())
            .releaseStatus(version.getReleaseStatus())
            .releaseNotesUrl(version.getReleaseNotesUrl())
            .releaseNotesGenerated(version.getReleaseNotesGenerated())
            .color(version.getColor())
            .createdBy(version.getCreatedBy())
            .updatedBy(version.getUpdatedBy())
            .releasedBy(version.getReleasedBy())
            .archivedBy(version.getArchivedBy())
            .createdAt(version.getCreatedAt())
            .updatedAt(version.getUpdatedAt())
            .overdue(version.getOverdue())
            .issueCount(fixVersionCount)
            .build();
    }

    private VersionMetricsResponse toMetricsResponse(VersionMetricSnapshot snapshot) {
        return VersionMetricsResponse.builder()
            .versionId(snapshot.getVersionId())
            .snapshotDate(snapshot.getSnapshotDate())
            .totalIssues(snapshot.getTotalIssues())
            .openIssues(snapshot.getOpenIssues())
            .closedIssues(snapshot.getClosedIssues())
            .resolvedIssues(snapshot.getResolvedIssues())
            .progressPercentage(snapshot.getProgressPercentage() != null ? snapshot.getProgressPercentage().doubleValue() : 0)
            .totalStoryPoints(snapshot.getTotalStoryPoints() != null ? snapshot.getTotalStoryPoints().doubleValue() : 0)
            .completedStoryPoints(snapshot.getCompletedStoryPoints() != null ? snapshot.getCompletedStoryPoints().doubleValue() : 0)
            .velocityPoints(snapshot.getVelocityPoints() != null ? snapshot.getVelocityPoints().doubleValue() : 0)
            .build();
    }

    private VersionDeploymentResponse toDeploymentResponse(VersionDeployment deployment) {
        return VersionDeploymentResponse.builder()
            .id(deployment.getId())
            .versionId(deployment.getVersionId())
            .deploymentId(deployment.getDeploymentId())
            .environment(deployment.getEnvironment())
            .deploymentUrl(deployment.getDeploymentUrl())
            .buildNumber(deployment.getBuildNumber())
            .buildUrl(deployment.getBuildUrl())
            .commitSha(deployment.getCommitSha())
            .deployedBy(deployment.getDeployedBy())
            .deployedAt(deployment.getDeployedAt())
            .status(deployment.getStatus())
            .metadata(deployment.getMetadata())
            .build();
    }

    private VersionBuildReferenceResponse toBuildReferenceResponse(VersionBuildReference build) {
        return VersionBuildReferenceResponse.builder()
            .id(build.getId())
            .versionId(build.getVersionId())
            .buildNumber(build.getBuildNumber())
            .buildUrl(build.getBuildUrl())
            .buildStatus(build.getBuildStatus())
            .branchName(build.getBranchName())
            .commitSha(build.getCommitSha())
            .commitMessage(build.getCommitMessage())
            .authorName(build.getAuthorName())
            .authorEmail(build.getAuthorEmail())
            .triggeredBy(build.getTriggeredBy())
            .triggeredAt(build.getTriggeredAt())
            .build();
    }

    private VersionReleaseNoteResponse toReleaseNoteResponse(VersionReleaseNote releaseNote) {
        return VersionReleaseNoteResponse.builder()
            .id(releaseNote.getId())
            .versionId(releaseNote.getVersionId())
            .content(releaseNote.getContent())
            .generatedAt(releaseNote.getGeneratedAt())
            .generatedBy(releaseNote.getGeneratedBy())
            .contentHash(releaseNote.getContentHash())
            .build();
    }

    // Getters for repositories (used in service methods)
    public IssueFixVersionRepository getFixVersionRepository() { return fixVersionRepository; }
    public IssueAffectsVersionRepository getAffectsVersionRepository() { return affectsVersionRepository; }
}