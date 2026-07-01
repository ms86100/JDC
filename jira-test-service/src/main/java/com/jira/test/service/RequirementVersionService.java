package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.entity.CoverageDriftRecord;
import com.jira.test.entity.RequirementChangeEvent;
import com.jira.test.entity.RequirementLink;
import com.jira.test.entity.RequirementVersion;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequirementVersionService {

    private final RequirementVersionRepository versionRepository;
    private final RequirementChangeEventRepository changeEventRepository;
    private final RequirementLinkRepository requirementLinkRepository;
    private final CoverageDriftRecordRepository driftRepository;
    private final ObjectMapper objectMapper;

    // ==================== Core Version Operations ====================

    /**
     * Create a new version for a requirement
     */
    @Transactional
    public RequirementVersion createVersion(UUID requirementId, String content, String changelog, UUID changedBy) {
        // Validate inputs
        validateRequirementContent(content);

        // Get previous version info
        Optional<RequirementVersion> previousVersion = getCurrentVersion(requirementId);
        int versionNum = getNextVersionNumber(requirementId);

        // Determine change magnitude based on content analysis
        ChangeMagnitudeResult magnitudeResult = determineChangeMagnitude(
            previousVersion.map(RequirementVersion::getContent).orElse(""),
            content,
            changelog
        );

        // Generate semantic version string
        String semanticVersion = generateSemanticVersion(versionNum, magnitudeResult.magnitude);

        // Build title and description snapshots
        String titleSnapshot = extractTitle(content);
        String descriptionSnapshot = extractDescription(content);

        RequirementVersion newVersion = RequirementVersion.builder()
                .requirementId(requirementId)
                .version(semanticVersion)
                .versionNumber(versionNum)
                .status(RequirementVersion.RequirementVersionStatus.DRAFT)
                .content(content)
                .changelog(changelog)
                .changeMagnitude(magnitudeResult.magnitude)
                .changedBy(changedBy)
                .titleSnapshot(titleSnapshot)
                .descriptionSnapshot(descriptionSnapshot)
                .createdAt(LocalDateTime.now())
                .build();

        // Link to previous version
        previousVersion.ifPresent(prev -> newVersion.setPreviousVersionId(prev.getId()));

        RequirementVersion saved = versionRepository.save(newVersion);

        // Record change event
        recordChangeEvent(requirementId, saved.getId(), RequirementChangeEvent.ChangeType.ADDED,
                List.of("version"), RequirementChangeEvent.ImpactLevel.LOW, null, versionNum);

        log.info("Created version {} for requirement {}", semanticVersion, requirementId);
        return saved;
    }

    /**
     * Publish a version - marks it as the current published version
     */
    @Transactional
    public RequirementVersion publishVersion(UUID versionId, UUID publishedBy) {
        RequirementVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("RequirementVersion", "id", versionId.toString()));

        // Validate version can be published
        validateVersionForPublishing(version);

        version.setStatus(RequirementVersion.RequirementVersionStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        version.setPublishedBy(publishedBy);

        // Capture linked tests snapshot at publish time
        captureLinkedTestsSnapshot(version);

        // Archive any previously published versions
        archiveOtherVersions(version.getRequirementId(), versionId);

        // Trigger coverage drift detection
        triggerCoverageDriftDetection(version.getRequirementId());

        log.info("Published version {} for requirement {}", version.getVersion(), version.getRequirementId());
        return versionRepository.save(version);
    }

    /**
     * Archive a version
     */
    @Transactional
    public RequirementVersion archiveVersion(UUID versionId) {
        RequirementVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("RequirementVersion", "id", versionId.toString()));

        if (version.getStatus() == RequirementVersion.RequirementVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot archive a published version. Unpublish it first.");
        }

        version.setStatus(RequirementVersion.RequirementVersionStatus.ARCHIVED);
        log.info("Archived version {} for requirement {}", version.getVersion(), version.getRequirementId());
        return versionRepository.save(version);
    }

    /**
     * Rollback to a specific version - creates a new version with content from the target
     */
    @Transactional
    public RequirementVersion rollbackToVersion(UUID requirementId, UUID targetVersionId, UUID changedBy) {
        RequirementVersion targetVersion = versionRepository.findById(targetVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("RequirementVersion", "id", targetVersionId.toString()));

        if (!targetVersion.getRequirementId().equals(requirementId)) {
            throw new IllegalArgumentException("Version does not belong to the specified requirement");
        }

        String rollbackChangelog = String.format("Rollback to version %s", targetVersion.getVersion());

        int newVersionNum = getNextVersionNumber(requirementId);
        RequirementVersion newVersion = RequirementVersion.builder()
                .requirementId(requirementId)
                .version(generateSemanticVersion(newVersionNum, RequirementVersion.ChangeMagnitude.MAJOR))
                .versionNumber(newVersionNum)
                .status(RequirementVersion.RequirementVersionStatus.DRAFT)
                .content(targetVersion.getContent())
                .changelog(rollbackChangelog)
                .changeMagnitude(RequirementVersion.ChangeMagnitude.MAJOR)
                .changedBy(changedBy)
                .previousVersionId(targetVersionId)
                .titleSnapshot(targetVersion.getTitleSnapshot())
                .descriptionSnapshot(targetVersion.getDescriptionSnapshot())
                .createdAt(LocalDateTime.now())
                .build();

        RequirementVersion saved = versionRepository.save(newVersion);

        // Record rollback event
        recordChangeEvent(requirementId, saved.getId(), RequirementChangeEvent.ChangeType.MODIFIED,
                List.of("content", "version"), RequirementChangeEvent.ImpactLevel.HIGH,
                targetVersion.getVersionNumber(), newVersionNum);

        log.info("Rolled back requirement {} to version {}", requirementId, targetVersion.getVersion());
        return saved;
    }

    // ==================== Version Query Operations ====================

    /**
     * Get version history for a requirement
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVersionHistory(UUID requirementId) {
        return versionRepository.findByRequirementIdOrderByCreatedAtDesc(requirementId).stream()
                .map(this::formatVersionSummary)
                .collect(Collectors.toList());
    }

    /**
     * Get the current published version for a requirement
     */
    @Transactional(readOnly = true)
    public Optional<RequirementVersion> getCurrentVersion(UUID requirementId) {
        return versionRepository.findFirstByRequirementIdAndStatusOrderByCreatedAtDesc(
                requirementId, RequirementVersion.RequirementVersionStatus.PUBLISHED);
    }

    /**
     * Get a specific version by ID
     */
    @Transactional(readOnly = true)
    public Optional<RequirementVersion> getVersion(UUID versionId) {
        return versionRepository.findById(versionId);
    }

    /**
     * Get all versions for a requirement with pagination
     */
    @Transactional(readOnly = true)
    public List<RequirementVersion> getVersions(UUID requirementId, int page, int size) {
        return versionRepository.findByRequirementIdOrderByCreatedAtDesc(requirementId)
                .stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Get version statistics for a requirement
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getVersionStats(UUID requirementId) {
        List<RequirementVersion> versions = versionRepository.findByRequirementIdOrderByCreatedAtDesc(requirementId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVersions", versions.size());
        stats.put("draftVersions", versions.stream()
                .filter(v -> v.getStatus() == RequirementVersion.RequirementVersionStatus.DRAFT).count());
        stats.put("publishedVersions", versions.stream()
                .filter(v -> v.getStatus() == RequirementVersion.RequirementVersionStatus.PUBLISHED).count());
        stats.put("archivedVersions", versions.stream()
                .filter(v -> v.getStatus() == RequirementVersion.RequirementVersionStatus.ARCHIVED).count());
        stats.put("latestVersion", versions.isEmpty() ? null : versions.get(0).getVersion());
        stats.put("firstCreatedAt", versions.isEmpty() ? null : versions.get(versions.size() - 1).getCreatedAt());

        // Calculate average changes per version
        long versionsWithChanges = versions.stream()
                .filter(v -> v.getChangelog() != null && !v.getChangelog().isEmpty())
                .count();
        stats.put("versionsWithChangelog", versionsWithChanges);

        return stats;
    }

    // ==================== Version Comparison ====================

    /**
     * Compare two versions and return detailed diff
     */
    @Transactional(readOnly = true)
    public Map<String, Object> compareVersions(UUID versionId1, UUID versionId2) {
        RequirementVersion v1 = versionRepository.findById(versionId1)
                .orElseThrow(() -> new ResourceNotFoundException("RequirementVersion", "id", versionId1.toString()));
        RequirementVersion v2 = versionRepository.findById(versionId2)
                .orElseThrow(() -> new ResourceNotFoundException("RequirementVersion", "id", versionId2.toString()));

        if (!v1.getRequirementId().equals(v2.getRequirementId())) {
            throw new IllegalArgumentException("Versions must belong to the same requirement");
        }

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("version1", formatVersionSummary(v1));
        comparison.put("version2", formatVersionSummary(v2));
        comparison.put("fieldDiffs", computeFieldDiffs(v1, v2));
        comparison.put("changeMagnitude", computeChangeMagnitudeFromDiffs(v1, v2));
        comparison.put("similarityScore", calculateSimilarityScore(v1, v2));
        comparison.put("affectedTests", findAffectedTests(v1.getRequirementId()));

        // Add lineage information
        comparison.put("lineage", buildVersionLineage(v1, v2));

        return comparison;
    }

    /**
     * Get diff between consecutive versions
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDiffFromPrevious(UUID versionId) {
        RequirementVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("RequirementVersion", "id", versionId.toString()));

        return versionRepository.findPreviousVersion(version.getRequirementId(), version.getVersionNumber())
                .map(prev -> compareVersions(prev.getId(), versionId))
                .orElse(Collections.emptyMap());
    }

    // ==================== Private Helper Methods ====================

    private int getNextVersionNumber(UUID requirementId) {
        return versionRepository.findMaxVersionByRequirementId(requirementId).orElse(0) + 1;
    }

    private void validateRequirementContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Requirement content cannot be empty");
        }
        if (content.length() > 1000000) { // 1MB limit
            throw new IllegalArgumentException("Requirement content exceeds maximum size of 1MB");
        }
    }

    private void validateVersionForPublishing(RequirementVersion version) {
        if (version.getStatus() == RequirementVersion.RequirementVersionStatus.PUBLISHED) {
            throw new IllegalStateException("Version is already published");
        }
        if (version.getStatus() == RequirementVersion.RequirementVersionStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot publish an archived version");
        }
        if (version.getContent() == null || version.getContent().trim().isEmpty()) {
            throw new IllegalStateException("Cannot publish a version with empty content");
        }
    }

    private ChangeMagnitudeResult determineChangeMagnitude(String previousContent, String newContent, String changelog) {
        RequirementVersion.ChangeMagnitude magnitude;
        String reason;

        // Calculate similarity score
        double similarity = calculateTextSimilarity(previousContent, newContent);
        int fieldChanges = countFieldChanges(previousContent, newContent);

        // Check for critical keywords in changelog
        boolean hasBreaking = changelog != null &&
            (changelog.toLowerCase().contains("breaking") ||
             changelog.toLowerCase().contains("deprecated") ||
             changelog.toLowerCase().contains("removed"));

        boolean hasMajor = changelog != null && changelog.toLowerCase().contains("major");

        if (fieldChanges >= 5 || hasBreaking) {
            magnitude = RequirementVersion.ChangeMagnitude.CRITICAL;
            reason = "Breaking changes detected";
        } else if (fieldChanges >= 3 || hasMajor || similarity < 0.7) {
            magnitude = RequirementVersion.ChangeMagnitude.MAJOR;
            reason = "Significant changes detected";
        } else {
            magnitude = RequirementVersion.ChangeMagnitude.MINOR;
            reason = "Minor changes or no significant changes";
        }

        return new ChangeMagnitudeResult(magnitude, reason, similarity);
    }

    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null && text2 == null) return 1.0;
        if (text1 == null || text2 == null) return 0.0;
        if (text1.equals(text2)) return 1.0;

        // Use Jaccard similarity for word sets
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) return 1.0;
        return (double) intersection.size() / union.size();
    }

    private int countFieldChanges(String oldContent, String newContent) {
        if (oldContent == null && newContent == null) return 0;
        if (oldContent == null || newContent == null) return 1;
        if (oldContent.equals(newContent)) return 0;

        // Estimate field changes by comparing sections
        String[] oldSections = oldContent.split("\\n\\n");
        String[] newSections = newContent.split("\\n\\n");

        int changes = 0;
        int maxSections = Math.max(oldSections.length, newSections.length);

        for (int i = 0; i < maxSections; i++) {
            String oldSec = i < oldSections.length ? oldSections[i] : "";
            String newSec = i < newSections.length ? newSections[i] : "";
            if (!oldSec.equals(newSec)) {
                changes++;
            }
        }

        return Math.max(1, changes);
    }

    private String generateSemanticVersion(int versionNum, RequirementVersion.ChangeMagnitude magnitude) {
        // Major version for CRITICAL, minor for MAJOR, patch for MINOR
        int major = magnitude == RequirementVersion.ChangeMagnitude.CRITICAL ? versionNum : 0;
        int minor = magnitude == RequirementVersion.ChangeMagnitude.MAJOR ? versionNum : 0;
        int patch = magnitude == RequirementVersion.ChangeMagnitude.MINOR ? versionNum : 0;

        if (major > 0) return major + ".0.0";
        if (minor > 0) return "1." + minor + ".0";
        return "1.0." + versionNum;
    }

    private void archiveOtherVersions(UUID requirementId, UUID currentVersionId) {
        List<RequirementVersion> publishedVersions = versionRepository
                .findByRequirementIdAndStatus(requirementId, RequirementVersion.RequirementVersionStatus.PUBLISHED);

        for (RequirementVersion v : publishedVersions) {
            if (!v.getId().equals(currentVersionId)) {
                v.setStatus(RequirementVersion.RequirementVersionStatus.ARCHIVED);
                versionRepository.save(v);
                log.info("Archived version {} when publishing new version", v.getVersion());
            }
        }
    }

    private void captureLinkedTestsSnapshot(RequirementVersion version) {
        List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(version.getRequirementId().toString());
        List<Map<String, Object>> linkedTests = links.stream()
                .map(link -> {
                    Map<String, Object> test = new HashMap<>();
                    test.put("testId", link.getTestId());
                    test.put("testKey", link.getTestId() != null ? link.getTestId().toString() : null);
                    test.put("linkType", link.getCoverageStatus());
                    return test;
                })
                .collect(Collectors.toList());

        try {
            version.setLinkedTestsSnapshot(objectMapper.writeValueAsString(linkedTests));
        } catch (JsonProcessingException e) {
            log.warn("Failed to capture linked tests snapshot: {}", e.getMessage());
        }
    }

    private void triggerCoverageDriftDetection(UUID requirementId) {
        // This would typically be async, but for simplicity we'll just log
        log.info("Coverage drift detection triggered for requirement {}", requirementId);
    }

    private Map<String, Object> formatVersionSummary(RequirementVersion version) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", version.getId());
        summary.put("version", version.getVersion());
        summary.put("versionNumber", version.getVersionNumber());
        summary.put("status", version.getStatus());
        summary.put("changeMagnitude", version.getChangeMagnitude());
        summary.put("changelog", version.getChangelog());
        summary.put("createdAt", version.getCreatedAt());
        summary.put("publishedAt", version.getPublishedAt());
        summary.put("publishedBy", version.getPublishedBy());
        summary.put("titleSnapshot", version.getTitleSnapshot());
        summary.put("descriptionSnapshot", version.getDescriptionSnapshot());
        summary.put("previousVersionId", version.getPreviousVersionId());

        // Add previous version info if available
        versionRepository.findPreviousVersion(version.getRequirementId(), version.getVersionNumber())
                .ifPresent(prev -> summary.put("previousVersion", prev.getVersion()));

        return summary;
    }

    private List<Map<String, String>> computeFieldDiffs(RequirementVersion v1, RequirementVersion v2) {
        List<Map<String, String>> diffs = new ArrayList<>();

        // Content diff
        if (!Objects.equals(v1.getContent(), v2.getContent())) {
            diffs.add(Map.of("field", "content", "type", "MODIFIED",
                    "v1Length", String.valueOf(v1.getContent() != null ? v1.getContent().length() : 0),
                    "v2Length", String.valueOf(v2.getContent() != null ? v2.getContent().length() : 0)));
        }

        // Title diff
        if (!Objects.equals(v1.getTitleSnapshot(), v2.getTitleSnapshot())) {
            diffs.add(Map.of("field", "title", "type", "MODIFIED",
                    "v1", v1.getTitleSnapshot() != null ? v1.getTitleSnapshot() : "",
                    "v2", v2.getTitleSnapshot() != null ? v2.getTitleSnapshot() : ""));
        }

        // Description diff
        if (!Objects.equals(v1.getDescriptionSnapshot(), v2.getDescriptionSnapshot())) {
            diffs.add(Map.of("field", "description", "type", "MODIFIED"));
        }

        // Changelog diff
        if (!Objects.equals(v1.getChangelog(), v2.getChangelog())) {
            diffs.add(Map.of("field", "changelog", "type", "MODIFIED"));
        }

        // Linked tests diff
        if (!Objects.equals(v1.getLinkedTestsSnapshot(), v2.getLinkedTestsSnapshot())) {
            diffs.add(Map.of("field", "linkedTests", "type", "MODIFIED"));
        }

        return diffs;
    }

    private String computeChangeMagnitudeFromDiffs(RequirementVersion v1, RequirementVersion v2) {
        List<Map<String, String>> diffs = computeFieldDiffs(v1, v2);
        int diffCount = diffs.size();

        if (diffCount >= 5) return "CRITICAL";
        if (diffCount >= 3) return "MAJOR";
        if (diffCount >= 1) return "MINOR";
        return "NONE";
    }

    private double calculateSimilarityScore(RequirementVersion v1, RequirementVersion v2) {
        double contentSim = calculateTextSimilarity(v1.getContent(), v2.getContent());
        double titleSim = calculateTextSimilarity(v1.getTitleSnapshot(), v2.getTitleSnapshot());
        double descSim = calculateTextSimilarity(v1.getDescriptionSnapshot(), v2.getDescriptionSnapshot());

        // Weighted average
        return (contentSim * 0.5 + titleSim * 0.25 + descSim * 0.25) * 100;
    }

    private List<Map<String, Object>> findAffectedTests(UUID requirementId) {
        List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(requirementId.toString());
        return links.stream()
                .map(link -> {
                    Map<String, Object> test = new HashMap<>();
                    test.put("testId", link.getTestId());
                    test.put("testKey", link.getTestId() != null ? link.getTestId().toString() : null);
                    test.put("linkType", link.getCoverageStatus());
                    return test;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildVersionLineage(RequirementVersion v1, RequirementVersion v2) {
        Map<String, Object> lineage = new HashMap<>();
        lineage.put("v1Version", v1.getVersion());
        lineage.put("v2Version", v2.getVersion());
        lineage.put("v1CreatedAt", v1.getCreatedAt());
        lineage.put("v2CreatedAt", v2.getCreatedAt());

        // Calculate version distance
        int distance = Math.abs(v1.getVersionNumber() - v2.getVersionNumber());
        lineage.put("versionDistance", distance);
        lineage.put("isConsecutive", distance == 1);

        return lineage;
    }

    private void recordChangeEvent(UUID requirementId, UUID versionId, RequirementChangeEvent.ChangeType changeType,
                                   List<String> affectedFields, RequirementChangeEvent.ImpactLevel impactLevel,
                                   Integer fromVersion, Integer toVersion) {
        RequirementChangeEvent event = RequirementChangeEvent.builder()
                .requirementId(requirementId)
                .versionId(versionId)
                .changeType(changeType)
                .affectedFields(serializeList(affectedFields))
                .impactLevel(impactLevel)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .build();
        changeEventRepository.save(event);
    }

    private String extractTitle(String content) {
        if (content == null || content.isEmpty()) return "";
        // Extract first line as title
        String[] lines = content.split("\\n", 2);
        return lines[0].trim();
    }

    private String extractDescription(String content) {
        if (content == null || content.isEmpty()) return "";
        // Extract everything after first line as description
        String[] lines = content.split("\\n", 2);
        return lines.length > 1 ? lines[1].trim() : "";
    }

    private String serializeList(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // ==================== Inner Classes ====================

    private static class ChangeMagnitudeResult {
        final RequirementVersion.ChangeMagnitude magnitude;
        final String reason;
        final double similarity;

        ChangeMagnitudeResult(RequirementVersion.ChangeMagnitude magnitude, String reason, double similarity) {
            this.magnitude = magnitude;
            this.reason = reason;
            this.similarity = similarity;
        }
    }
}