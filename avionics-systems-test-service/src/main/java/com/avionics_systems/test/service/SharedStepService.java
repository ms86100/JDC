package com.avionics_systems.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.*;
import com.avionics_systems.test.exception.*;
import com.avionics_systems.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SharedStepService {

    private final SharedStepRepository sharedStepRepository;
    private final SharedStepVersionRepository versionRepository;
    private final TestSharedStepMappingRepository mappingRepository;
    private final SharedStepDependencyRepository dependencyRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestStepRepository testStepRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.defaults.shared-step.risk-low-threshold:0.3}")
    private double riskLowThreshold;

    @Value("${app.defaults.shared-step.risk-medium-threshold:0.6}")
    private double riskMediumThreshold;

    @Value("${app.defaults.shared-step.risk-high-threshold:0.8}")
    private double riskHighThreshold;

    @Value("${app.defaults.shared-step.search-page-size:20}")
    private int defaultSearchPageSize;

    @Value("${app.defaults.shared-step.default-fuzzy-threshold:0.7}")
    private double defaultFuzzyThreshold;

    // ==================== Shared Step CRUD ====================

    @Transactional
    public SharedStepResponse createSharedStep(CreateSharedStepRequest request) {
        log.info("Creating shared step: {} for project: {}", request.getName(), request.getProjectId());

        if (sharedStepRepository.existsByProjectIdAndNameAndArchivedFalse(request.getProjectId(), request.getName())) {
            throw new DuplicateResourceException("Shared step with name '" + request.getName() + "' already exists");
        }

        String stepsJson = serializeSteps(request.getSteps());

        SharedStep sharedStep = SharedStep.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .steps(stepsJson)
                .currentVersion(1)
                .usageCount(0)
                .folderId(request.getFolderId())
                .build();

        sharedStep = sharedStepRepository.save(sharedStep);

        // Create initial version
        createVersionSnapshot(sharedStep.getId(), request.getSteps(), "Initial version", null);

        log.info("Shared step created with id: {}", sharedStep.getId());
        return mapToSharedStepResponse(sharedStep);
    }

    @Transactional(readOnly = true)
    public SharedStepResponse getSharedStep(UUID sharedStepId) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));
        return mapToSharedStepResponse(sharedStep);
    }

    @Transactional(readOnly = true)
    public SharedStepDetailResponse getSharedStepDetail(UUID sharedStepId) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        List<SharedStepVersion> versions = versionRepository.findBySharedStepIdOrderByVersionNumberDesc(sharedStepId);
        List<SharedStepImpactResponse> impacts = getImpactAnalysis(sharedStepId);
        List<SharedStepDependencyResponse> dependencies = getDependencyTree(sharedStepId);

        return SharedStepDetailResponse.builder()
                .id(sharedStep.getId())
                .projectId(sharedStep.getProjectId())
                .name(sharedStep.getName())
                .description(sharedStep.getDescription())
                .steps(parseSteps(sharedStep.getSteps()))
                .stepCount(parseSteps(sharedStep.getSteps()).size())
                .currentVersion(sharedStep.getCurrentVersion())
                .totalVersions(versions.size())
                .tags(new ArrayList<>())
                .categories(new ArrayList<>())
                .labels(new ArrayList<>())
                .usageCount(sharedStep.getUsageCount())
                .activeTestsCount(impacts.size())
                .folderId(sharedStep.getFolderId())
                .createdBy(sharedStep.getCreatedBy())
                .createdAt(sharedStep.getCreatedAt())
                .updatedAt(sharedStep.getUpdatedAt())
                .recentVersions(versions.stream().limit(5).map(v ->
                        SharedStepDetailResponse.VersionSummary.builder()
                                .versionNumber(v.getVersionNumber())
                                .changeSummary(v.getChangeSummary())
                                .createdAt(v.getCreatedAt())
                                .createdBy(v.getCreatedBy())
                                .isCurrent(v.getIsCurrent())
                                .build()
                ).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<SharedStepResponse> getSharedStepsByProject(UUID projectId) {
        return sharedStepRepository.findByProjectIdAndArchivedFalseOrderByUsageCountDesc(projectId).stream()
                .map(this::mapToSharedStepResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SharedStepResponse> searchSharedSteps(UUID projectId, String search) {
        return sharedStepRepository.searchByNameOrDescription(projectId, search).stream()
                .map(this::mapToSharedStepResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SharedStepResponse updateSharedStep(UUID sharedStepId, CreateSharedStepRequest request) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        if (request.getName() != null) sharedStep.setName(request.getName());
        if (request.getDescription() != null) sharedStep.setDescription(request.getDescription());
        if (request.getSteps() != null) {
            sharedStep.setSteps(serializeSteps(request.getSteps()));
            // Create new version
            createVersionSnapshot(sharedStep.getId(), request.getSteps(), "Steps updated", null);
        }

        sharedStep = sharedStepRepository.save(sharedStep);
        log.info("Shared step updated: {}", sharedStepId);
        return mapToSharedStepResponse(sharedStep);
    }

    @Transactional
    public SharedStepResponse updateSharedStepTags(UUID sharedStepId, SharedStepTagRequest request) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        sharedStep = sharedStepRepository.save(sharedStep);
        log.info("Updated tags for shared step: {}", sharedStepId);
        return mapToSharedStepResponse(sharedStep);
    }

    @Transactional
    public void deleteSharedStep(UUID sharedStepId) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        // Check usage
        long usageCount = mappingRepository.countBySharedStepId(sharedStepId);
        if (usageCount > 0) {
            throw new InvalidOperationException(
                    "Cannot delete shared step that is used by " + usageCount + " tests. Remove usage first.");
        }

        sharedStep.setArchived(true);
        sharedStepRepository.save(sharedStep);
        log.info("Shared step archived: {}", sharedStepId);
    }

    @Transactional
    public void forceDeleteSharedStep(UUID sharedStepId, boolean cascade) {
        SharedStep sharedStep = sharedStepRepository.findById(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        if (cascade) {
            // Remove all mappings first
            List<TestSharedStepMapping> mappings = mappingRepository.findBySharedStepId(sharedStepId);
            mappingRepository.deleteAll(mappings);
        }

        // Delete versions
        List<SharedStepVersion> versions = versionRepository.findBySharedStepIdOrderByVersionNumberDesc(sharedStepId);
        versionRepository.deleteAll(versions);

        // Delete dependencies
        List<SharedStepDependency> dependencies = new ArrayList<>();
        dependencies.addAll(dependencyRepository.findByParentSharedStepId(sharedStepId));
        dependencies.addAll(dependencyRepository.findByChildSharedStepId(sharedStepId));
        dependencyRepository.deleteAll(dependencies);

        // Delete shared step
        sharedStepRepository.delete(sharedStep);
        log.info("Force deleted shared step: {} with cascade: {}", sharedStepId, cascade);
    }

    // ==================== Version History with Diff ====================

    private SharedStepVersionResponse createVersionSnapshot(UUID sharedStepId, List<SharedStepDto> steps,
                                                           String changeSummary, UUID createdBy) {
        SharedStep sharedStep = sharedStepRepository.findById(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        int newVersionNum = versionRepository.findMaxVersionBySharedStepId(sharedStepId).orElse(0) + 1;

        // Mark previous version as not current
        versionRepository.findBySharedStepIdAndIsCurrentTrue(sharedStepId)
                .ifPresent(v -> {
                    v.setIsCurrent(false);
                    versionRepository.save(v);
                });

        SharedStepVersion version = SharedStepVersion.builder()
                .sharedStepId(sharedStepId)
                .versionNumber(newVersionNum)
                .steps(serializeSteps(steps))
                .changeSummary(changeSummary)
                .createdBy(createdBy)
                .isCurrent(true)
                .build();

        version = versionRepository.save(version);

        sharedStep.setCurrentVersion(newVersionNum);
        sharedStepRepository.save(sharedStep);

        return mapToVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public List<SharedStepVersionResponse> getVersionHistory(UUID sharedStepId) {
        return versionRepository.findBySharedStepIdOrderByVersionNumberDesc(sharedStepId).stream()
                .map(this::mapToVersionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SharedStepVersionResponse getVersion(UUID sharedStepId, Integer version) {
        SharedStepVersion versionEntity = versionRepository.findBySharedStepIdAndVersionNumber(sharedStepId, version)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStepVersion",
                        "sharedStepId=" + sharedStepId + " and version=" + version, null));
        return mapToVersionResponse(versionEntity);
    }

    @Transactional
    public SharedStepVersionResponse createNewVersion(UUID sharedStepId, List<SharedStepDto> steps, String changeSummary) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        return createVersionSnapshot(sharedStepId, steps, changeSummary, null);
    }

    @Transactional(readOnly = true)
    public SharedStepVersionDiffResponse getVersionDiff(UUID sharedStepId, Integer fromVersion, Integer toVersion) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        SharedStepVersion fromVer = versionRepository.findBySharedStepIdAndVersionNumber(sharedStepId, fromVersion)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStepVersion", "version", fromVersion));
        SharedStepVersion toVer = versionRepository.findBySharedStepIdAndVersionNumber(sharedStepId, toVersion)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStepVersion", "version", toVersion));

        List<SharedStepDto> fromSteps = parseSteps(fromVer.getSteps());
        List<SharedStepDto> toSteps = parseSteps(toVer.getSteps());

        // Calculate diff
        List<SharedStepVersionDiffResponse.StepChange> changes = calculateStepChanges(fromSteps, toSteps);

        int added = (int) changes.stream().filter(c -> "ADDED".equals(c.getChangeType())).count();
        int removed = (int) changes.stream().filter(c -> "REMOVED".equals(c.getChangeType())).count();
        int modified = (int) changes.stream().filter(c -> "MODIFIED".equals(c.getChangeType())).count();

        String magnitude = (added + removed + modified) <= 2 ? "MINOR" :
                          (added + removed + modified) <= 5 ? "MODERATE" : "MAJOR";

        boolean breakingChange = modified > 0 || removed > 0;

        return SharedStepVersionDiffResponse.builder()
                .sharedStepId(sharedStepId)
                .sharedStepName(sharedStep.getName())
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .stepsAdded(added)
                .stepsRemoved(removed)
                .stepsModified(modified)
                .stepChanges(changes)
                .fromVersionDetails(mapToVersionResponse(fromVer))
                .toVersionDetails(mapToVersionResponse(toVer))
                .changeMagnitude(magnitude)
                .breakingChange(breakingChange)
                .build();
    }

    private List<SharedStepVersionDiffResponse.StepChange> calculateStepChanges(
            List<SharedStepDto> fromSteps, List<SharedStepDto> toSteps) {
        List<SharedStepVersionDiffResponse.StepChange> changes = new ArrayList<>();
        Map<Integer, SharedStepDto> fromMap = fromSteps.stream()
                .collect(Collectors.toMap(SharedStepDto::getOrder, s -> s));
        Map<Integer, SharedStepDto> toMap = toSteps.stream()
                .collect(Collectors.toMap(SharedStepDto::getOrder, s -> s));

        // Find removed and modified
        for (SharedStepDto from : fromSteps) {
            SharedStepDto to = toMap.get(from.getOrder());
            if (to == null) {
                changes.add(SharedStepVersionDiffResponse.StepChange.builder()
                        .stepOrder(from.getOrder())
                        .changeType("REMOVED")
                        .stepType(from.getStepType())
                        .beforeDescription(from.getDescription())
                        .beforeExpectedResult(from.getExpectedResult())
                        .build());
            } else if (!stepsEqual(from, to)) {
                List<String> changedFields = new ArrayList<>();
                if (!Objects.equals(from.getDescription(), to.getDescription())) changedFields.add("description");
                if (!Objects.equals(from.getExpectedResult(), to.getExpectedResult())) changedFields.add("expectedResult");
                if (!Objects.equals(from.getStepType(), to.getStepType())) changedFields.add("stepType");

                changes.add(SharedStepVersionDiffResponse.StepChange.builder()
                        .stepOrder(from.getOrder())
                        .changeType("MODIFIED")
                        .stepType(to.getStepType())
                        .beforeDescription(from.getDescription())
                        .afterDescription(to.getDescription())
                        .beforeExpectedResult(from.getExpectedResult())
                        .afterExpectedResult(to.getExpectedResult())
                        .changedFields(changedFields)
                        .similarityScore(calculateSimilarity(from, to))
                        .build());
            }
        }

        // Find added
        for (SharedStepDto to : toSteps) {
            if (!fromMap.containsKey(to.getOrder())) {
                changes.add(SharedStepVersionDiffResponse.StepChange.builder()
                        .stepOrder(to.getOrder())
                        .changeType("ADDED")
                        .stepType(to.getStepType())
                        .afterDescription(to.getDescription())
                        .afterExpectedResult(to.getExpectedResult())
                        .build());
            }
        }

        return changes;
    }

    private boolean stepsEqual(SharedStepDto a, SharedStepDto b) {
        return Objects.equals(a.getDescription(), b.getDescription()) &&
               Objects.equals(a.getExpectedResult(), b.getExpectedResult()) &&
               Objects.equals(a.getStepType(), b.getStepType());
    }

    private double calculateSimilarity(SharedStepDto a, SharedStepDto b) {
        if (a.getDescription() == null || b.getDescription() == null) return 0.0;
        String s1 = a.getDescription().toLowerCase();
        String s2 = b.getDescription().toLowerCase();
        if (s1.equals(s2)) return 1.0;
        // Simple Jaccard similarity for words
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Transactional
    public SharedStepVersionResponse revertToVersion(UUID sharedStepId, Integer versionNumber) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        SharedStepVersion targetVersion = versionRepository.findBySharedStepIdAndVersionNumber(sharedStepId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStepVersion", "version", versionNumber));

        List<SharedStepDto> steps = parseSteps(targetVersion.getSteps());

        return createVersionSnapshot(sharedStepId, steps, "Reverted to version " + versionNumber, null);
    }

    // ==================== Impact Analysis ====================

    @Transactional(readOnly = true)
    public List<SharedStepImpactResponse> getImpactAnalysis(UUID sharedStepId) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        List<TestSharedStepMapping> mappings = mappingRepository.findBySharedStepId(sharedStepId);
        List<UUID> testIds = mappings.stream().map(TestSharedStepMapping::getTestId).distinct().collect(Collectors.toList());

        List<TestIssue> tests = testIssueRepository.findAllById(testIds);

        return mappings.stream()
                .map(mapping -> {
                    TestIssue test = tests.stream()
                            .filter(t -> t.getId().equals(mapping.getTestId()))
                            .findFirst().orElse(null);
                    if (test == null) return null;
                    return SharedStepImpactResponse.builder()
                            .testId(test.getId())
                            .testIssueKey(test.getName())
                            .testName(test.getName())
                            .usageCount(1)
                            .lastUsedAt(mapping.getCreatedAt() != null ? mapping.getCreatedAt().toString() : null)
                            .status(test.getStatus())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FullImpactAnalysisResponse getFullImpactAnalysis(UUID sharedStepId) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        List<TestSharedStepMapping> mappings = mappingRepository.findBySharedStepId(sharedStepId);
        List<UUID> testIds = mappings.stream().map(TestSharedStepMapping::getTestId).distinct().collect(Collectors.toList());
        List<TestIssue> tests = testIssueRepository.findAllById(testIds);

        Map<String, Integer> testsByStatus = tests.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus() != null ? t.getStatus() : "UNKNOWN",
                        Collectors.summingInt(t -> 1)));

        int lowImpact = 0, mediumImpact = 0, highImpact = 0;
        for (UUID testId : testIds) {
            long count = mappings.stream().filter(m -> m.getTestId().equals(testId)).count();
            if (count <= 1) lowImpact++;
            else if (count <= 5) mediumImpact++;
            else highImpact++;
        }

        double impactScore = testIds.isEmpty() ? 0.0 :
                Math.min(1.0, (double) testIds.size() / 100.0 * (1.0 + highImpact * 0.1));

        String riskLevel = impactScore < riskLowThreshold ? "LOW" : impactScore < riskMediumThreshold ? "MEDIUM" : impactScore < riskHighThreshold ? "HIGH" : "CRITICAL";

        List<FullImpactAnalysisResponse.ImpactDetail> details = mappings.stream()
                .map(mapping -> {
                    TestIssue test = tests.stream()
                            .filter(t -> t.getId().equals(mapping.getTestId()))
                            .findFirst().orElse(null);
                    if (test == null) return null;
                    long usageCount = mappings.stream().filter(m -> m.getTestId().equals(test.getId())).count();
                    return FullImpactAnalysisResponse.ImpactDetail.builder()
                            .testId(test.getId())
                            .testName(test.getName())
                            .testStatus(test.getStatus())
                            .priority(parsePriority(test.getPriority()))
                            .mappingCount((int) usageCount)
                            .createdAt(test.getCreatedAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return FullImpactAnalysisResponse.builder()
                .sharedStepId(sharedStepId)
                .sharedStepName(sharedStep.getName())
                .currentVersion(sharedStep.getCurrentVersion())
                .totalAffectedTests(testIds.size())
                .testsByStatus(testsByStatus)
                .distribution(FullImpactAnalysisResponse.ImpactDistribution.builder()
                        .lowImpact(lowImpact)
                        .mediumImpact(mediumImpact)
                        .highImpact(highImpact)
                        .impactScore(impactScore)
                        .build())
                .riskAssessment(FullImpactAnalysisResponse.RiskAssessment.builder()
                        .riskLevel(riskLevel)
                        .riskScore(impactScore)
                        .riskFactors(highImpact > 0 ? List.of("High-impact tests affected") : List.of())
                        .mitigationSteps(impactScore > 0.5 ? List.of("Review affected tests before updating",
                                "Consider creating new version instead of modifying") : List.of())
                        .build())
                .affectedTests(details)
                .recommendations(generateImpactRecommendations(impactScore, testIds.size(), highImpact))
                .build();
    }

    private List<String> generateImpactRecommendations(double impactScore, int testCount, int highImpact) {
        List<String> recommendations = new ArrayList<>();
        if (impactScore > 0.8) {
            recommendations.add("Critical impact - consider duplicating this shared step before modifying");
            recommendations.add("Notify all test owners before making changes");
        } else if (impactScore > 0.5) {
            recommendations.add("High impact - create a new version and migrate tests gradually");
            recommendations.add("Review all affected tests for potential impacts");
        }
        if (highImpact > 0) {
            recommendations.add("Some tests have multiple references - changes may have amplified effects");
        }
        if (testCount == 0) {
            recommendations.add("This shared step is not used - safe to modify without coordination");
        }
        return recommendations;
    }

    @Transactional(readOnly = true)
    public List<UUID> getAffectedTestIds(UUID sharedStepId) {
        return mappingRepository.findTestIdsBySharedStepId(sharedStepId);
    }

    // ==================== Dependency Graph ====================

    @Transactional(readOnly = true)
    public List<SharedStepDependencyResponse> getDependencyTree(UUID sharedStepId) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        List<SharedStepDependency> children = dependencyRepository.findByParentSharedStepId(sharedStepId);
        List<SharedStepDependency> parents = dependencyRepository.findByChildSharedStepId(sharedStepId);

        List<SharedStepDependencyResponse> responses = new ArrayList<>();

        for (SharedStepDependency child : children) {
            SharedStep childStep = sharedStepRepository.findByIdAndArchivedFalse(child.getChildSharedStepId()).orElse(null);
            if (childStep != null) {
                responses.add(SharedStepDependencyResponse.builder()
                        .id(child.getId())
                        .parentSharedStepId(sharedStepId)
                        .parentName(sharedStep.getName())
                        .childSharedStepId(child.getChildSharedStepId())
                        .childName(childStep.getName())
                        .dependencyType(child.getDependencyType())
                        .depth(1)
                        .hasCircularDependency(false)
                        .build());
            }
        }

        for (SharedStepDependency parent : parents) {
            SharedStep parentStep = sharedStepRepository.findByIdAndArchivedFalse(parent.getParentSharedStepId()).orElse(null);
            if (parentStep != null) {
                responses.add(SharedStepDependencyResponse.builder()
                        .id(parent.getId())
                        .parentSharedStepId(parent.getParentSharedStepId())
                        .parentName(parentStep.getName())
                        .childSharedStepId(sharedStepId)
                        .childName(sharedStep.getName())
                        .dependencyType(parent.getDependencyType())
                        .depth(1)
                        .hasCircularDependency(false)
                        .build());
            }
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public DependencyGraphResponse getFullDependencyGraph(UUID sharedStepId) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", sharedStepId));

        Map<UUID, DependencyGraphResponse.GraphNode> nodeMap = new HashMap<>();
        List<DependencyGraphResponse.GraphEdge> edges = new ArrayList<>();
        Set<String> circularPaths = new HashSet<>();
        int maxDepth = 0;

        // Build graph recursively
        buildDependencyGraph(sharedStepId, 0, nodeMap, edges, circularPaths, new HashSet<>());

        for (DependencyGraphResponse.GraphNode node : nodeMap.values()) {
            maxDepth = Math.max(maxDepth, node.getDepth());
        }

        String complexity = nodeMap.size() <= 5 ? "LOW" : nodeMap.size() <= 15 ? "MEDIUM" : "HIGH";
        double maintainabilityScore = calculateMaintainabilityScore(nodeMap.size(), maxDepth, circularPaths.size());

        return DependencyGraphResponse.builder()
                .sharedStepId(sharedStepId)
                .sharedStepName(sharedStep.getName())
                .currentVersion(sharedStep.getCurrentVersion())
                .allNodes(new ArrayList<>(nodeMap.values()))
                .allEdges(edges)
                .totalDependencies(edges.size())
                .directDependencies((int) edges.stream().filter(e -> e.getWeight() == 1).count())
                .transitiveDependencies((int) edges.stream().filter(e -> e.getWeight() > 1).count())
                .dependentSteps((int) nodeMap.values().stream().filter(n -> !n.getId().equals(sharedStepId) && n.getUsageCount() > 0).count())
                .maxDepth(maxDepth)
                .hasCircularDependency(!circularPaths.isEmpty())
                .circularPaths(new ArrayList<>(circularPaths))
                .complexityLevel(complexity)
                .maintainabilityScore(maintainabilityScore)
                .build();
    }

    private void buildDependencyGraph(UUID currentId, int depth, Map<UUID, DependencyGraphResponse.GraphNode> nodeMap,
                                      List<DependencyGraphResponse.GraphEdge> edges, Set<String> circularPaths,
                                      Set<UUID> visited) {
        if (visited.contains(currentId)) {
            circularPaths.add("Circular dependency detected involving " + currentId);
            return;
        }

        if (nodeMap.containsKey(currentId)) {
            // Already visited, update depth if needed
            DependencyGraphResponse.GraphNode existing = nodeMap.get(currentId);
            if (depth < existing.getDepth()) {
                existing.setDepth(depth);
            }
            return;
        }

        visited.add(currentId);

        SharedStep current = sharedStepRepository.findByIdAndArchivedFalse(currentId)
                .orElse(sharedStepRepository.findById(currentId).orElse(null));

        if (current == null) return;

        int usageCount = (int) mappingRepository.countBySharedStepId(currentId);

        nodeMap.put(currentId, DependencyGraphResponse.GraphNode.builder()
                .id(currentId)
                .name(current.getName())
                .version(current.getCurrentVersion())
                .nodeType("SHARED_STEP")
                .depth(depth)
                .isRoot(depth == 0)
                .usageCount(usageCount)
                .status(current.getArchived() ? "ARCHIVED" : "ACTIVE")
                .build());

        // Find dependencies
        List<SharedStepDependency> children = dependencyRepository.findByParentSharedStepId(currentId);
        for (SharedStepDependency dep : children) {
            edges.add(DependencyGraphResponse.GraphEdge.builder()
                    .sourceId(currentId)
                    .targetId(dep.getChildSharedStepId())
                    .edgeType("DEPENDS_ON")
                    .weight(1)
                    .build());
            buildDependencyGraph(dep.getChildSharedStepId(), depth + 1, nodeMap, edges, circularPaths, new HashSet<>(visited));
        }
    }

    private double calculateMaintainabilityScore(int nodeCount, int maxDepth, int circularCount) {
        double score = 1.0;
        score -= Math.min(0.3, nodeCount * 0.02);
        score -= Math.min(0.3, maxDepth * 0.1);
        score -= circularCount * 0.2;
        return Math.max(0.0, Math.min(1.0, score));
    }

    @Transactional
    public void validateNoCircularDependencies(UUID sharedStepId, List<SharedStepDto> newSteps) {
        Set<UUID> referencedIds = extractReferencedSharedSteps(newSteps);
        referencedIds.add(sharedStepId);

        Set<UUID> visited = new HashSet<>();
        Set<UUID> recursionStack = new HashSet<>();

        for (UUID refId : referencedIds) {
            if (hasCircularDependencyDFS(refId, sharedStepId, visited, recursionStack, new HashSet<>())) {
                throw new InvalidOperationException("Circular dependency detected in shared steps");
            }
        }
    }

    private boolean hasCircularDependencyDFS(UUID currentId, UUID targetId,
                                             Set<UUID> visited, Set<UUID> recursionStack,
                                             Set<UUID> checked) {
        if (currentId.equals(targetId) && !checked.contains(currentId)) {
            return true;
        }

        if (recursionStack.contains(currentId)) {
            return true;
        }

        if (visited.contains(currentId)) {
            return false;
        }

        visited.add(currentId);
        recursionStack.add(currentId);

        List<UUID> childIds = dependencyRepository.findChildIdsByParentId(currentId);
        for (UUID childId : childIds) {
            if (hasCircularDependencyDFS(childId, targetId, visited, recursionStack, checked)) {
                return true;
            }
        }

        recursionStack.remove(currentId);
        return false;
    }

    private Set<UUID> extractReferencedSharedSteps(List<SharedStepDto> steps) {
        Set<UUID> refs = new HashSet<>();
        return refs;
    }

    // ==================== Search with Fuzzy Matching ====================

    @Transactional(readOnly = true)
    public SharedStepSearchResponse searchSharedStepsAdvanced(SharedStepSearchRequest request) {
        long startTime = System.currentTimeMillis();

        List<SharedStep> allSteps = sharedStepRepository.findByProjectIdAndArchivedFalse(request.getProjectId());

        // Apply filters
        List<SharedStep> filtered = allSteps.stream()
                .filter(ss -> {
                    if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                        String query = request.getQuery().toLowerCase();
                        String name = ss.getName() != null ? ss.getName().toLowerCase() : "";
                        String desc = ss.getDescription() != null ? ss.getDescription().toLowerCase() : "";

                        if (Boolean.TRUE.equals(request.getFuzzyMatch())) {
                            double threshold = request.getFuzzyThreshold() != null ? request.getFuzzyThreshold() : defaultFuzzyThreshold;
                            return fuzzyMatch(query, name) >= threshold ||
                                   fuzzyMatch(query, desc) >= threshold ||
                                   name.contains(query) || desc.contains(query);
                        }
                        return name.contains(query) || desc.contains(query);
                    }
                    return true;
                })
                .filter(ss -> {
                    if (request.getMinUsageCount() != null && ss.getUsageCount() < request.getMinUsageCount()) {
                        return false;
                    }
                    if (request.getMaxUsageCount() != null && ss.getUsageCount() > request.getMaxUsageCount()) {
                        return false;
                    }
                    return true;
                })
                // tags not stored on SharedStep entity
                .collect(Collectors.toList());

        // Sort
        Comparator<SharedStep> comparator;
        switch (request.getSortBy() != null ? request.getSortBy() : "name") {
            case "usageCount":
                comparator = Comparator.comparing(SharedStep::getUsageCount);
                break;
            case "updatedAt":
                comparator = Comparator.comparing(SharedStep::getUpdatedAt);
                break;
            case "createdAt":
                comparator = Comparator.comparing(SharedStep::getCreatedAt);
                break;
            case "relevance":
                comparator = (a, b) -> {
                    double scoreA = calculateRelevanceScore(a, request.getQuery());
                    double scoreB = calculateRelevanceScore(b, request.getQuery());
                    return Double.compare(scoreB, scoreA);
                };
                break;
            default:
                comparator = Comparator.comparing(SharedStep::getName);
        }

        if ("DESC".equalsIgnoreCase(request.getSortOrder())) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator);

        int totalCount = filtered.size();
        int page = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : defaultSearchPageSize;

        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        List<SharedStepResponse> results = new ArrayList<>();
        if (fromIndex < filtered.size()) {
            results = filtered.subList(fromIndex, toIndex).stream()
                    .map(this::mapToSharedStepResponse)
                    .collect(Collectors.toList());
        }

        long duration = System.currentTimeMillis() - startTime;

        return SharedStepSearchResponse.builder()
                .results(results)
                .totalCount(totalCount)
                .page(page)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) totalCount / pageSize))
                .searchQuery(request.getQuery())
                .fuzzyMatchThreshold(request.getFuzzyThreshold())
                .searchDurationMs(duration)
                .facets(buildSearchFacets(allSteps))
                .build();
    }

    private double fuzzyMatch(String query, String text) {
        if (text == null || query == null) return 0.0;
        if (query.equalsIgnoreCase(text)) return 1.0;

        // Levenshtein-based similarity
        int distance = levenshteinDistance(query.toLowerCase(), text.toLowerCase());
        int maxLen = Math.max(query.length(), text.length());
        return maxLen == 0 ? 0.0 : 1.0 - (double) distance / maxLen;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private double calculateRelevanceScore(SharedStep ss, String query) {
        if (query == null || query.isEmpty()) return ss.getUsageCount();
        double score = 0.0;
        if (ss.getName() != null && ss.getName().toLowerCase().contains(query.toLowerCase())) {
            score += 10.0;
        }
        if (ss.getDescription() != null && ss.getDescription().toLowerCase().contains(query.toLowerCase())) {
            score += 5.0;
        }
        score += ss.getUsageCount() * 0.1;
        return score;
    }

    private SharedStepSearchResponse.SearchFacets buildSearchFacets(List<SharedStep> steps) {
        Set<String> allTags = new HashSet<>();
        Set<String> allCategories = new HashSet<>();
        long highUsage = 0, mediumUsage = 0, lowUsage = 0, unused = 0;

        for (SharedStep ss : steps) {
            int usage = ss.getUsageCount() != null ? ss.getUsageCount() : 0;
            if (usage >= 50) highUsage++;
            else if (usage >= 10) mediumUsage++;
            else if (usage >= 1) lowUsage++;
            else unused++;
        }

        return SharedStepSearchResponse.SearchFacets.builder()
                .availableTags(new ArrayList<>(allTags))
                .availableCategories(new ArrayList<>(allCategories))
                .usageRange(SharedStepSearchResponse.UsageRangeFacet.builder()
                        .high(highUsage)
                        .medium(mediumUsage)
                        .low(lowUsage)
                        .unused(unused)
                        .build())
                .build();
    }

    // ==================== Bulk Operations ====================

    @Transactional
    public SharedStepBulkResponse performBulkOperation(SharedStepBulkRequest request) {
        List<SharedStepBulkResponse.BulkOperationResult> results = new ArrayList<>();
        Map<String, SharedStepBulkResponse.OperationSummary> summaries = new HashMap<>();
        long startTime = System.currentTimeMillis();

        int successCount = 0, failureCount = 0, warningCount = 0;

        for (UUID sharedStepId : request.getSharedStepIds()) {
            try {
                SharedStep sharedStep = sharedStepRepository.findById(sharedStepId).orElse(null);
                if (sharedStep == null) {
                    results.add(SharedStepBulkResponse.BulkOperationResult.builder()
                            .sharedStepId(sharedStepId)
                            .success(false)
                            .message("Shared step not found")
                            .build());
                    failureCount++;
                    continue;
                }

                List<String> warnings = new ArrayList<>();
                List<String> affectedTests = new ArrayList<>();

                switch (request.getOperation()) {
                    case "UPDATE":
                        performBulkUpdate(sharedStep, request);
                        break;
                    case "ARCHIVE":
                        sharedStep.setArchived(true);
                        sharedStepRepository.save(sharedStep);
                        break;
                    case "DELETE":
                        long usage = mappingRepository.countBySharedStepId(sharedStepId);
                        if (usage > 0 && !Boolean.TRUE.equals(request.getForceOperation())) {
                            warnings.add("Shared step is used by " + usage + " tests");
                            if (!Boolean.TRUE.equals(request.getSkipValidation())) {
                                throw new InvalidOperationException("Cannot delete shared step in use");
                            }
                        }
                        sharedStep.setArchived(true);
                        sharedStepRepository.save(sharedStep);
                        break;
                    case "TAG":
                        performBulkTag(sharedStep, request);
                        break;
                    case "MIGRATE":
                        List<String> migratedTests = performBulkMigrate(sharedStep, request);
                        affectedTests.addAll(migratedTests);
                        break;
                    default:
                        throw new InvalidOperationException("Unknown operation: " + request.getOperation());
                }

                results.add(SharedStepBulkResponse.BulkOperationResult.builder()
                        .sharedStepId(sharedStepId)
                        .sharedStepName(sharedStep.getName())
                        .success(true)
                        .message("Operation completed successfully")
                        .warnings(warnings)
                        .affectedTests(affectedTests)
                        .build());
                successCount++;
                warningCount += warnings.size();

            } catch (Exception e) {
                log.error("Bulk operation failed for shared step {}: {}", sharedStepId, e.getMessage());
                results.add(SharedStepBulkResponse.BulkOperationResult.builder()
                        .sharedStepId(sharedStepId)
                        .success(false)
                        .message("Operation failed: " + e.getMessage())
                        .build());
                failureCount++;
            }
        }

        summaries.put(request.getOperation(), SharedStepBulkResponse.OperationSummary.builder()
                .operation(request.getOperation())
                .totalProcessed(request.getSharedStepIds().size())
                .succeeded(successCount)
                .failed(failureCount)
                .warnings(warningCount)
                .build());

        long duration = System.currentTimeMillis() - startTime;

        return SharedStepBulkResponse.builder()
                .totalRequested(request.getSharedStepIds().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .warningCount(warningCount)
                .results(results)
                .operationSummaries(summaries)
                .durationMs(duration)
                .build();
    }

    private void performBulkUpdate(SharedStep sharedStep, SharedStepBulkRequest request) {
        if (request.getNewName() != null) sharedStep.setName(request.getNewName());
        if (request.getNewDescription() != null) sharedStep.setDescription(request.getNewDescription());
        if (request.getNewFolderId() != null) sharedStep.setFolderId(request.getNewFolderId());
        sharedStepRepository.save(sharedStep);
    }

    private void performBulkTag(SharedStep sharedStep, SharedStepBulkRequest request) {
        sharedStepRepository.save(sharedStep);
    }

    private List<String> performBulkMigrate(SharedStep sharedStep, SharedStepBulkRequest request) {
        List<String> migratedTests = new ArrayList<>();

        if (request.getTargetVersion() == null) {
            throw new InvalidOperationException("Target version is required for migration");
        }

        List<TestSharedStepMapping> mappings = mappingRepository.findBySharedStepId(sharedStep.getId());
        SharedStepVersion targetVersion = versionRepository
                .findBySharedStepIdAndVersionNumber(sharedStep.getId(), request.getTargetVersion())
                .orElseThrow(() -> new ResourceNotFoundException("SharedStepVersion", "version", request.getTargetVersion()));

        for (TestSharedStepMapping mapping : mappings) {
            if (true) {
                mapping.setSharedStepVersionId(targetVersion.getId());
                mapping.setEmbeddedSnapshot(targetVersion.getSteps());
                mappingRepository.save(mapping);
                migratedTests.add(mapping.getTestId().toString());
            }
        }

        return migratedTests;
    }

    // ==================== Migration ====================

    @Transactional
    public SharedStepMigrationResponse migrateToVersion(SharedStepMigrationRequest request) {
        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(request.getSharedStepId())
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", request.getSharedStepId()));

        SharedStepVersion toVersion = versionRepository
                .findBySharedStepIdAndVersionNumber(request.getSharedStepId(), request.getToVersion())
                .orElseThrow(() -> new ResourceNotFoundException("SharedStepVersion", "version", request.getToVersion()));

        SharedStepVersionDiffResponse diff = null;
        if (request.getFromVersion() != null) {
            diff = getVersionDiff(request.getSharedStepId(), request.getFromVersion(), request.getToVersion());
        }

        List<TestSharedStepMapping> mappings = mappingRepository.findBySharedStepId(request.getSharedStepId());
        List<UUID> targetTestIds = request.getTestIds() != null ?
                request.getTestIds() :
                mappings.stream().map(TestSharedStepMapping::getTestId).distinct().collect(Collectors.toList());

        List<SharedStepMigrationResponse.MigratedTest> migratedTests = new ArrayList<>();
        List<SharedStepMigrationResponse.MigrationFailure> failures = new ArrayList<>();

        for (UUID testId : targetTestIds) {
            try {
                TestIssue test = testIssueRepository.findById(testId).orElse(null);
                if (test == null) {
                    failures.add(SharedStepMigrationResponse.MigrationFailure.builder()
                            .testId(testId)
                            .errorCode("NOT_FOUND")
                            .errorMessage("Test not found")
                            .build());
                    continue;
                }

                List<TestSharedStepMapping> testMappings = mappings.stream()
                        .filter(m -> m.getTestId().equals(testId))
                        .collect(Collectors.toList());

                for (TestSharedStepMapping mapping : testMappings) {
                    String oldVersion = mapping.getSharedStepVersionId() != null ?
                            versionRepository.findById(mapping.getSharedStepVersionId())
                                    .map(v -> "v" + v.getVersionNumber())
                                    .orElse("unknown") :
                            "v" + sharedStep.getCurrentVersion();

                    mapping.setSharedStepVersionId(toVersion.getId());
                    mapping.setEmbeddedSnapshot(toVersion.getSteps());
                    mappingRepository.save(mapping);

                    migratedTests.add(SharedStepMigrationResponse.MigratedTest.builder()
                            .testId(testId)
                            .testName(test.getName())
                            .mappingsUpdated(testMappings.size())
                            .testNeedsReview(diff != null && diff.getBreakingChange())
                            .previousVersion(oldVersion)
                            .newVersion("v" + toVersion.getVersionNumber())
                            .build());
                }
            } catch (Exception e) {
                failures.add(SharedStepMigrationResponse.MigrationFailure.builder()
                        .testId(testId)
                        .errorCode("MIGRATION_ERROR")
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return SharedStepMigrationResponse.builder()
                .sharedStepId(request.getSharedStepId())
                .sharedStepName(sharedStep.getName())
                .fromVersion(request.getFromVersion() != null ? request.getFromVersion() : sharedStep.getCurrentVersion())
                .toVersion(request.getToVersion())
                .testsMigrated(migratedTests.size())
                .testsFailed(failures.size())
                .migratedTests(migratedTests)
                .failures(failures)
                .versionDiff(diff)
                .migratedAt(LocalDateTime.now())
                .migrationReason(request.getMigrationReason())
                .totalUsageCountAffected(mappings.size())
                .build();
    }

    // ==================== Templates ====================

    @Transactional
    public SharedStepTemplateResponse createTemplate(SharedStepTemplateRequest request) {
        UUID templateId = UUID.randomUUID();

        return SharedStepTemplateResponse.builder()
                .id(templateId)
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .steps(request.getSteps())
                .stepCount(request.getSteps() != null ? request.getSteps().size() : 0)
                .tags(request.getTags())
                .labels(request.getLabels())
                .instructions(request.getInstructions())
                .variables(request.getVariables())
                .usageCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SharedStepTemplateResponse> getTemplates(UUID projectId, String category) {
        // In a real implementation, this would fetch from a template repository
        return new ArrayList<>();
    }

    @Transactional
    public SharedStepResponse createFromTemplate(UUID templateId, UUID projectId, Map<String, String> variables) {
        // In a real implementation, this would fetch the template and substitute variables
        CreateSharedStepRequest request = CreateSharedStepRequest.builder()
                .projectId(projectId)
                .name("New Shared Step from Template")
                .steps(new ArrayList<>())
                .build();
        return createSharedStep(request);
    }

    // ==================== Test Integration ====================

    @Transactional(readOnly = true)
    public List<EmbeddedStepResponse> getEmbeddedSteps(UUID testId) {
        List<TestSharedStepMapping> mappings = mappingRepository.findByTestIdOrderByTestStepIndexAsc(testId);

        return mappings.stream().map(mapping -> {
            SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(mapping.getSharedStepId()).orElse(null);
            if (sharedStep == null) return null;

            SharedStepVersion version = mapping.getSharedStepVersionId() != null ?
                    versionRepository.findById(mapping.getSharedStepVersionId()).orElse(null) :
                    versionRepository.findBySharedStepIdAndIsCurrentTrue(mapping.getSharedStepId()).orElse(null);

            return EmbeddedStepResponse.builder()
                    .id(mapping.getId())
                    .testId(testId)
                    .stepIndex(mapping.getTestStepIndex())
                    .sharedStepId(sharedStep.getId())
                    .sharedStepName(sharedStep.getName())
                    .sharedStepVersion(version != null ? version.getVersionNumber() : sharedStep.getCurrentVersion())
                    .embeddedSteps(parseSteps(version != null ? version.getSteps() : sharedStep.getSteps()))
                    .createdAt(mapping.getCreatedAt() != null ? mapping.getCreatedAt().toString() : null)
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional
    public EmbeddedStepResponse insertSharedStep(InsertSharedStepRequest request) {
        TestIssue test = testIssueRepository.findById(request.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", request.getTestId()));

        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(request.getSharedStepId())
                .orElseThrow(() -> new ResourceNotFoundException("SharedStep", "id", request.getSharedStepId()));

        SharedStepVersion version = request.getSharedStepVersionId() != null ?
                versionRepository.findById(request.getSharedStepVersionId())
                        .orElseThrow(() -> new ResourceNotFoundException("SharedStepVersion", "id", request.getSharedStepVersionId())) :
                versionRepository.findBySharedStepIdAndIsCurrentTrue(request.getSharedStepId())
                        .orElseThrow(() -> new ResourceNotFoundException("SharedStepVersion", "sharedStepId", request.getSharedStepId()));

        TestSharedStepMapping mapping = TestSharedStepMapping.builder()
                .testId(request.getTestId())
                .testStepIndex(request.getPosition())
                .sharedStepId(request.getSharedStepId())
                .sharedStepVersionId(version.getId())
                .embeddedSnapshot(version.getSteps())
                .parameters(request.getParameters() != null ? serializeMap(request.getParameters()) : null)
                .build();

        mapping = mappingRepository.save(mapping);

        sharedStep.setUsageCount((int) mappingRepository.countBySharedStepId(sharedStep.getId()));
        sharedStepRepository.save(sharedStep);

        log.info("Inserted shared step {} at position {} in test {}", request.getSharedStepId(), request.getPosition(), request.getTestId());

        return EmbeddedStepResponse.builder()
                .id(mapping.getId())
                .testId(request.getTestId())
                .stepIndex(request.getPosition())
                .sharedStepId(sharedStep.getId())
                .sharedStepName(sharedStep.getName())
                .sharedStepVersion(version.getVersionNumber())
                .embeddedSteps(parseSteps(version.getSteps()))
                .createdAt(mapping.getCreatedAt() != null ? mapping.getCreatedAt().toString() : null)
                .build();
    }

    @Transactional
    public void removeSharedStep(UUID mappingId) {
        TestSharedStepMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSharedStepMapping", "id", mappingId));

        UUID sharedStepId = mapping.getSharedStepId();
        mappingRepository.delete(mapping);

        SharedStep sharedStep = sharedStepRepository.findByIdAndArchivedFalse(sharedStepId).orElse(null);
        if (sharedStep != null) {
            sharedStep.setUsageCount((int) mappingRepository.countBySharedStepId(sharedStepId));
            sharedStepRepository.save(sharedStep);
        }

        log.info("Removed shared step mapping: {}", mappingId);
    }

    // ==================== Snapshot for Execution ====================

    @Transactional(readOnly = true)
    public String snapshotForExecution(UUID testId, UUID executionId) {
        List<TestSharedStepMapping> mappings = mappingRepository.findByTestIdOrderByTestStepIndexAsc(testId);

        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (TestSharedStepMapping mapping : mappings) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("stepIndex", mapping.getTestStepIndex());
            entry.put("sharedStepId", mapping.getSharedStepId());
            entry.put("embeddedSteps", parseSteps(mapping.getEmbeddedSnapshot()));
            entry.put("parameters", parseMap(mapping.getParameters()));
            snapshot.add(entry);
        }

        return serializeMapList(snapshot);
    }

    // ==================== Helper Methods ====================

    private String serializeSteps(List<SharedStepDto> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize steps: " + e.getMessage());
        }
    }

    private List<SharedStepDto> parseSteps(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SharedStepDto>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse steps: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String serializeMap(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize map: " + e.getMessage());
        }
    }

    private Map<String, String> parseMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse map: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String serializeMapList(List<Map<String, Object>> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to serialize: " + e.getMessage());
        }
    }

    private SharedStepResponse mapToSharedStepResponse(SharedStep sharedStep) {
        List<SharedStepVersion> versions = versionRepository.findBySharedStepIdOrderByVersionNumberDesc(sharedStep.getId());
        List<SharedStepImpactResponse> impact = getImpactAnalysis(sharedStep.getId());

        return SharedStepResponse.builder()
                .id(sharedStep.getId())
                .projectId(sharedStep.getProjectId())
                .name(sharedStep.getName())
                .description(sharedStep.getDescription())
                .steps(parseSteps(sharedStep.getSteps()))
                .currentVersion(sharedStep.getCurrentVersion())
                .usageCount(sharedStep.getUsageCount())
                .folderId(sharedStep.getFolderId())
                .createdAt(sharedStep.getCreatedAt())
                .updatedAt(sharedStep.getUpdatedAt())
                .versions(versions.stream().map(this::mapToVersionResponse).collect(Collectors.toList()))
                .impact(impact)
                .build();
    }

    private SharedStepVersionResponse mapToVersionResponse(SharedStepVersion version) {
        return SharedStepVersionResponse.builder()
                .id(version.getId())
                .sharedStepId(version.getSharedStepId())
                .versionNumber(version.getVersionNumber())
                .steps(parseSteps(version.getSteps()))
                .changeSummary(version.getChangeSummary())
                .createdBy(version.getCreatedBy())
                .isCurrent(version.getIsCurrent())
                .createdAt(version.getCreatedAt())
                .build();
    }

    private Integer parsePriority(String priority) {
        if (priority == null) {
            return 0;
        }
        return switch (priority.toUpperCase()) {
            case "BLOCKER" -> 4;
            case "CRITICAL" -> 3;
            case "HIGH" -> 2;
            case "MEDIUM" -> 1;
            default -> 0;
        };
    }
}