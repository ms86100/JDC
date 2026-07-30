package com.avionics_systems.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.test.entity.CoverageDriftRecord;
import com.avionics_systems.test.entity.RequirementLink;
import com.avionics_systems.test.entity.RequirementVersion;
import com.avionics_systems.test.entity.TestIssue;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
public class CoverageDriftService {

    private final CoverageDriftRecordRepository driftRepository;
    private final RequirementLinkRepository requirementLinkRepository;
    private final TestIssueRepository testIssueRepository;
    private final RequirementVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    private static final BigDecimal DRIFT_THRESHOLD = new BigDecimal("10.00");
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("25.00");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("5.00");

    // ==================== Core Drift Operations ====================

    /**
     * Detect coverage drift for a specific requirement
     */
    @Transactional
    public CoverageDriftRecord detectDrift(UUID requirementId) {
        return detectDrift(requirementId, null);
    }

    /**
     * Detect coverage drift for a specific requirement with optional project context
     */
    @Transactional
    public CoverageDriftRecord detectDrift(UUID requirementId, UUID projectId) {
        List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(requirementId.toString());

        // Get current coverage from the latest published version if available
        BigDecimal currentCoverage = getCoverageFromVersion(requirementId)
                .orElseGet(() -> calculateCoverageScore(links));

        BigDecimal previousCoverage = getPreviousCoverage(requirementId);
        BigDecimal drift = calculateDrift(previousCoverage, currentCoverage);

        // Determine drift type and severity
        CoverageDriftRecord.DriftType driftType = determineDriftType(drift);
        DriftSeverity severity = determineSeverity(drift);

        // Identify affected tests
        List<Map<String, Object>> affectedTests = links.stream()
                .map(link -> {
                    Map<String, Object> testInfo = new HashMap<>();
                    testInfo.put("testId", link.getTestId());
                    testInfo.put("testKey", link.getTestId() != null ? link.getTestId().toString() : null);
                    testInfo.put("linkType", link.getCoverageStatus());
                    testInfo.put("status", assessTestStatus(link));
                    return testInfo;
                })
                .collect(Collectors.toList());

        // Find missing and stale tests
        List<String> missingTests = identifyMissingTests(requirementId);
        List<String> staleTests = identifyStaleTests(requirementId, links);

        // Determine if action is required based on severity
        boolean actionRequired = severity == DriftSeverity.CRITICAL || severity == DriftSeverity.HIGH;

        UUID effectiveProjectId = projectId != null ? projectId : getProjectIdFromLinks(links);

        CoverageDriftRecord record = CoverageDriftRecord.builder()
                .requirementId(requirementId)
                .projectId(effectiveProjectId)
                .previousCoverage(previousCoverage)
                .currentCoverage(currentCoverage)
                .drift(drift)
                .driftType(driftType)
                .previousTestCount(getPreviousTestCount(requirementId))
                .currentTestCount(links.size())
                .affectedTests(serializeList(affectedTests))
                .missingCoverage(serializeList(missingTests))
                .staleCoverage(serializeList(staleTests))
                .actionRequired(actionRequired)
                .build();

        CoverageDriftRecord saved = driftRepository.save(record);
        log.info("Detected drift for requirement {}: {}% ({})", requirementId, drift, severity);

        return saved;
    }

    /**
     * Record drift for a requirement (manual trigger)
     */
    @Transactional
    public CoverageDriftRecord recordDrift(UUID requirementId) {
        return detectDrift(requirementId, null);
    }

    /**
     * Detect all drifts for a project
     */
    @Transactional
    public List<CoverageDriftRecord> detectAllDrifts(UUID projectId) {
        List<CoverageDriftRecord> drifts = new ArrayList<>();

        // Get all requirement IDs from links for this project
        Set<UUID> requirementIds = requirementLinkRepository
                .findByProjectId(projectId).stream()
                .map(link -> {
                    try {
                        return UUID.fromString(link.getRequirementKey());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Also check requirements from version history
        requirementIds.addAll(getRequirementsFromVersions(projectId));

        for (UUID requirementId : requirementIds) {
            try {
                drifts.add(detectDrift(requirementId, projectId));
            } catch (Exception e) {
                log.warn("Failed to detect drift for requirement {}: {}", requirementId, e.getMessage());
            }
        }

        log.info("Detected {} drifts for project {}", drifts.size(), projectId);
        return drifts;
    }

    // ==================== Drift Query Operations ====================

    /**
     * Get drift history for a project over specified days
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDriftHistory(UUID projectId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<CoverageDriftRecord> records = driftRepository
                .findByProjectIdAndDetectedAtAfterOrderByDetectedAtDesc(projectId, since);

        return records.stream()
                .map(this::formatDriftRecord)
                .collect(Collectors.toList());
    }

    /**
     * Get drift alerts above threshold
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDriftAlerts(UUID projectId, BigDecimal threshold) {
        BigDecimal alertThreshold = threshold != null ? threshold : DRIFT_THRESHOLD;

        List<CoverageDriftRecord> records = driftRepository.findByProjectIdAndActionRequiredTrue(projectId);

        return records.stream()
                .filter(r -> r.getDrift() != null && r.getDrift().abs().compareTo(alertThreshold) >= 0)
                .map(this::formatDriftRecord)
                .collect(Collectors.toList());
    }

    /**
     * Get drift summary for a project
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDriftSummary(UUID projectId) {
        List<CoverageDriftRecord> recentRecords = driftRepository
                .findAllByProjectIdOrderByDetectedAtDesc(projectId, PageRequest.of(0, 100));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDrifts", recentRecords.size());
        summary.put("improvedCount", recentRecords.stream()
                .filter(r -> r.getDriftType() == CoverageDriftRecord.DriftType.IMPROVED).count());
        summary.put("degradedCount", recentRecords.stream()
                .filter(r -> r.getDriftType() == CoverageDriftRecord.DriftType.DEGRADED).count());
        summary.put("stableCount", recentRecords.stream()
                .filter(r -> r.getDriftType() == CoverageDriftRecord.DriftType.STABLE).count());
        summary.put("actionRequiredCount", recentRecords.stream()
                .filter(r -> Boolean.TRUE.equals(r.getActionRequired())).count());

        // Calculate average drift
        BigDecimal avgDrift = recentRecords.stream()
                .map(CoverageDriftRecord::getDrift)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(recentRecords.size(), 1)), 2, RoundingMode.HALF_UP);
        summary.put("averageDrift", avgDrift);

        // Get most degraded requirements
        List<Map<String, Object>> mostDegraded = recentRecords.stream()
                .filter(r -> r.getDrift() != null && r.getDrift().compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparing(CoverageDriftRecord::getDrift))
                .limit(5)
                .map(r -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("requirementId", r.getRequirementId());
                    row.put("drift", r.getDrift());
                    row.put("currentCoverage", r.getCurrentCoverage());
                    return row;
                })
                .collect(Collectors.toList());
        summary.put("mostDegraded", mostDegraded);

        return summary;
    }

    /**
     * Get coverage trend for a requirement over specified days
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCoverageTrend(UUID requirementId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<CoverageDriftRecord> records = driftRepository
                .findByRequirementIdAndDetectedAtAfterOrderByDetectedAtAsc(requirementId, since);

        List<Map<String, Object>> trendData = records.stream()
                .map(r -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("timestamp", r.getDetectedAt());
                    point.put("coverage", r.getCurrentCoverage());
                    point.put("drift", r.getDrift());
                    point.put("driftType", r.getDriftType());
                    return point;
                })
                .collect(Collectors.toList());

        // Calculate trend statistics
        Map<String, Object> trend = new HashMap<>();
        trend.put("requirementId", requirementId);
        trend.put("periodDays", days);
        trend.put("dataPoints", trendData);
        trend.put("dataPointCount", records.size());
        trend.put("averageCoverage", calculateAverageCoverage(records));
        trend.put("maxDrift", findMaxDrift(records));
        trend.put("minDrift", findMinDrift(records));
        trend.put("stabilityScore", calculateStabilityScore(records));
        trend.put("coverageTrend", determineCoverageTrend(records));

        return trend;
    }

    // ==================== Remediation Operations ====================

    /**
     * Auto-remediate - suggest tests to add based on coverage gaps
     */
    @Transactional(readOnly = true)
    public Map<String, Object> autoRemediate(UUID requirementId) {
        List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(requirementId.toString());
        List<String> missingTests = identifyMissingTests(requirementId);
        List<String> staleTests = identifyStaleTests(requirementId, links);

        List<Map<String, Object>> suggestions = new ArrayList<>();

        // Generate suggestions for missing tests
        for (String missing : missingTests) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("type", "ADD_TEST");
            suggestion.put("target", missing);
            suggestion.put("reason", "Coverage gap detected - test is missing but required");
            suggestion.put("priority", "HIGH");
            suggestion.put("estimatedImpact", calculateCoverageImpact(missing));
            suggestions.add(suggestion);
        }

        // Generate suggestions for stale tests
        for (String stale : staleTests) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("type", "UPDATE_TEST");
            suggestion.put("target", stale);
            suggestion.put("reason", "Test appears stale - requirement may have changed");
            suggestion.put("priority", "MEDIUM");
            suggestion.put("action", "Review and update test to match current requirement version");
            suggestions.add(suggestion);
        }

        // Suggest re-linking orphaned tests
        List<Map<String, Object>> orphanedTests = findOrphanedTests(requirementId);
        for (Map<String, Object> orphaned : orphanedTests) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("type", "RELINK_TEST");
            suggestion.put("target", orphaned.get("testKey"));
            suggestion.put("reason", "Test exists but is not linked to current requirement");
            suggestion.put("priority", "LOW");
            suggestions.add(suggestion);
        }

        Map<String, Object> remediation = new HashMap<>();
        remediation.put("requirementId", requirementId);
        remediation.put("suggestions", suggestions);
        remediation.put("totalSuggestions", suggestions.size());
        remediation.put("estimatedCoverageImprovement", calculateTotalCoverageImprovement(suggestions));
        remediation.put("highPriorityCount", suggestions.stream()
                .filter(s -> "HIGH".equals(s.get("priority"))).count());

        return remediation;
    }

    /**
     * Apply automatic remediation
     */
    @Transactional
    public Map<String, Object> applyRemediation(UUID requirementId, List<String> suggestionTypes) {
        Map<String, Object> results = new HashMap<>();
        List<String> applied = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        Map<String, Object> remediation = autoRemediate(requirementId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> suggestions = (List<Map<String, Object>>) remediation.get("suggestions");

        for (Map<String, Object> suggestion : suggestions) {
            String type = (String) suggestion.get("type");
            if (suggestionTypes.contains(type)) {
                try {
                    // In a real implementation, this would trigger actual test creation/linkage
                    applied.add(type + ": " + suggestion.get("target"));
                    log.info("Applied remediation: {} for {}", type, suggestion.get("target"));
                } catch (Exception e) {
                    failed.add(type + ": " + suggestion.get("target") + " - " + e.getMessage());
                }
            }
        }

        // Re-detect drift after remediation
        CoverageDriftRecord newDrift = detectDrift(requirementId);

        results.put("requirementId", requirementId);
        results.put("appliedCount", applied.size());
        results.put("failedCount", failed.size());
        results.put("applied", applied);
        results.put("failed", failed);
        results.put("newDrift", newDrift.getDrift());
        results.put("newCoverage", newDrift.getCurrentCoverage());

        return results;
    }

    // ==================== Private Helper Methods ====================

    private Optional<BigDecimal> getCoverageFromVersion(UUID requirementId) {
        return versionRepository.findFirstByRequirementIdAndStatusOrderByCreatedAtDesc(
                        requirementId, RequirementVersion.RequirementVersionStatus.PUBLISHED)
                .map(version -> {
                    // Calculate coverage based on linked tests snapshot
                    try {
                        if (version.getLinkedTestsSnapshot() != null) {
                            List<?> tests = objectMapper.readValue(version.getLinkedTestsSnapshot(), List.class);
                            return BigDecimal.valueOf(Math.min(tests.size() * 10, 100))
                                    .setScale(2, RoundingMode.HALF_UP);
                        }
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to parse linked tests snapshot: {}", e.getMessage());
                    }
                    return BigDecimal.ZERO;
                });
    }

    private BigDecimal calculateCoverageScore(List<RequirementLink> links) {
        if (links.isEmpty()) return BigDecimal.ZERO;

        int linkedTests = links.size();
        int totalWeight = links.stream()
                .mapToInt(link -> "BLOCKER".equals(link.getCoverageStatus()) ? 3 :
                        "CRITICAL".equals(link.getCoverageStatus()) ? 2 : 1)
                .sum();

        // Weighted coverage calculation
        double maxCoverage = linkedTests * 10.0;
        double actualCoverage = Math.min(totalWeight * 3.33, maxCoverage);

        return BigDecimal.valueOf(actualCoverage)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getPreviousCoverage(UUID requirementId) {
        return driftRepository.findFirstByRequirementIdOrderByDetectedAtDesc(requirementId)
                .map(CoverageDriftRecord::getCurrentCoverage)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateDrift(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(previous).setScale(2, RoundingMode.HALF_UP);
    }

    private CoverageDriftRecord.DriftType determineDriftType(BigDecimal drift) {
        if (drift == null) return CoverageDriftRecord.DriftType.STABLE;
        if (drift.compareTo(BigDecimal.ZERO) > 0) {
            return CoverageDriftRecord.DriftType.IMPROVED;
        } else if (drift.compareTo(BigDecimal.ZERO) < 0) {
            return CoverageDriftRecord.DriftType.DEGRADED;
        }
        return CoverageDriftRecord.DriftType.STABLE;
    }

    private DriftSeverity determineSeverity(BigDecimal drift) {
        if (drift == null) return DriftSeverity.LOW;
        BigDecimal absDrift = drift.abs();

        if (absDrift.compareTo(CRITICAL_THRESHOLD) >= 0) {
            return DriftSeverity.CRITICAL;
        } else if (absDrift.compareTo(DRIFT_THRESHOLD) >= 0) {
            return DriftSeverity.HIGH;
        } else if (absDrift.compareTo(WARNING_THRESHOLD) >= 0) {
            return DriftSeverity.MEDIUM;
        }
        return DriftSeverity.LOW;
    }

    private String assessTestStatus(RequirementLink link) {
        // Assess test health based on various factors
        if (link.getCreatedAt() != null && link.getCreatedAt().isBefore(LocalDateTime.now().minusMonths(6))) {
            return "STALE";
        }
        return "ACTIVE";
    }

    private List<String> identifyMissingTests(UUID requirementId) {
        // In a real implementation, this would compare against a test specification
        // For now, return empty list - would be populated from test templates/rules
        return Collections.emptyList();
    }

    private List<String> identifyStaleTests(UUID requirementId, List<RequirementLink> links) {
        // Check for tests that haven't been executed recently
        return links.stream()
                .filter(link -> link.getCreatedAt() != null &&
                        link.getCreatedAt().isBefore(LocalDateTime.now().minusMonths(6)))
                .map(link -> link.getTestId() != null ? link.getTestId().toString() : null)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> findOrphanedTests(UUID requirementId) {
        // Find tests that exist but aren't linked
        return Collections.emptyList();
    }

    private Set<UUID> getRequirementsFromVersions(UUID projectId) {
        // Get requirements from version history
        return new HashSet<>();
    }

    private UUID getProjectIdFromLinks(List<RequirementLink> links) {
        return links.isEmpty() ? null : null;
    }

    private Integer getPreviousTestCount(UUID requirementId) {
        return driftRepository.findFirstByRequirementIdOrderByDetectedAtDesc(requirementId)
                .map(CoverageDriftRecord::getCurrentTestCount)
                .orElse(0);
    }

    private Map<String, Object> formatDriftRecord(CoverageDriftRecord record) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("id", record.getId());
        formatted.put("requirementId", record.getRequirementId());
        formatted.put("projectId", record.getProjectId());
        formatted.put("drift", record.getDrift());
        formatted.put("driftType", record.getDriftType());
        formatted.put("previousCoverage", record.getPreviousCoverage());
        formatted.put("currentCoverage", record.getCurrentCoverage());
        formatted.put("previousTestCount", record.getPreviousTestCount());
        formatted.put("currentTestCount", record.getCurrentTestCount());
        formatted.put("detectedAt", record.getDetectedAt());
        formatted.put("actionRequired", record.getActionRequired());

        // Parse affected tests for better formatting
        try {
            if (record.getAffectedTests() != null) {
                List<?> tests = objectMapper.readValue(record.getAffectedTests(), List.class);
                formatted.put("affectedTestCount", tests.size());
            }
        } catch (JsonProcessingException e) {
            formatted.put("affectedTestCount", 0);
        }

        return formatted;
    }

    private BigDecimal calculateAverageCoverage(List<CoverageDriftRecord> records) {
        if (records.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = records.stream()
                .map(CoverageDriftRecord::getCurrentCoverage)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal findMaxDrift(List<CoverageDriftRecord> records) {
        return records.stream()
                .map(CoverageDriftRecord::getDrift)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal findMinDrift(List<CoverageDriftRecord> records) {
        return records.stream()
                .map(CoverageDriftRecord::getDrift)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateStabilityScore(List<CoverageDriftRecord> records) {
        if (records.size() < 2) return BigDecimal.valueOf(100);

        long stableCount = records.stream()
                .filter(r -> r.getDriftType() == CoverageDriftRecord.DriftType.STABLE)
                .count();

        return BigDecimal.valueOf((stableCount * 100.0) / records.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String determineCoverageTrend(List<CoverageDriftRecord> records) {
        if (records.size() < 2) return "INSUFFICIENT_DATA";

        BigDecimal first = records.get(0).getCurrentCoverage();
        BigDecimal last = records.get(records.size() - 1).getCurrentCoverage();

        if (first == null || last == null) return "UNKNOWN";

        int comparison = last.compareTo(first);
        if (comparison > 0) return "IMPROVING";
        if (comparison < 0) return "DECLINING";
        return "STABLE";
    }

    private BigDecimal calculateCoverageImpact(String missingTest) {
        return BigDecimal.valueOf(5.0).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalCoverageImprovement(List<Map<String, Object>> suggestions) {
        return BigDecimal.valueOf(suggestions.stream()
                .filter(s -> "ADD_TEST".equals(s.get("type")))
                .count() * 5.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String serializeList(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // ==================== Enums ====================

    private enum DriftSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}