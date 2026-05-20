package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ==================== Versioning ====================

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
                            .status(test.getStatus())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UUID> getAffectedTestIds(UUID sharedStepId) {
        return mappingRepository.findTestIdsBySharedStepId(sharedStepId);
    }

    // ==================== Dependencies ====================

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

    @Transactional
    public void validateNoCircularDependencies(UUID sharedStepId, List<SharedStepDto> newSteps) {
        // Extract referenced shared step IDs from steps
        Set<UUID> referencedIds = extractReferencedSharedSteps(newSteps);
        referencedIds.add(sharedStepId); // Add self to check for self-reference

        // Check for circular dependency using DFS
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
            return true; // Circular dependency found
        }

        if (recursionStack.contains(currentId)) {
            return true;
        }

        if (visited.contains(currentId)) {
            return false;
        }

        visited.add(currentId);
        recursionStack.add(currentId);

        // Check dependencies of current shared step
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
        // Extract shared step IDs from parameters or custom fields if present
        // For now, this returns empty - actual implementation would scan step content
        return refs;
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
                        .orElseThrow(() -> new ResourceNotFoundException("No current version found"));

        // Create mapping
        TestSharedStepMapping mapping = TestSharedStepMapping.builder()
                .testId(request.getTestId())
                .testStepIndex(request.getPosition())
                .sharedStepId(request.getSharedStepId())
                .sharedStepVersionId(version.getId())
                .embeddedSnapshot(version.getSteps())
                .parameters(request.getParameters() != null ? serializeMap(request.getParameters()) : null)
                .build();

        mapping = mappingRepository.save(mapping);

        // Update usage count
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

        // Update usage count
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
}