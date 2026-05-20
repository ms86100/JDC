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

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImpactAnalysisService {

    private final ComponentRepository componentRepository;
    private final TestComponentMappingRepository testComponentMappingRepository;
    private final CodeChangeEventRepository codeChangeEventRepository;
    private final ImpactAnalysisResultRepository impactAnalysisResultRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestDatasetRepository datasetRepository;
    private final ObjectMapper objectMapper;

    // ==================== Component Management ====================

    @Transactional
    public ComponentResponse registerComponent(ComponentRequest request) {
        log.info("Registering component: {} for project: {}", request.getComponentName(), request.getProjectId());

        if (componentRepository.findByProjectIdAndComponentName(request.getProjectId(), request.getComponentName()).isPresent()) {
            throw new DuplicateResourceException("Component '" + request.getComponentName() + "' already exists in this project");
        }

        Component component = Component.builder()
                .projectId(request.getProjectId())
                .componentName(request.getComponentName())
                .componentPath(request.getComponentPath())
                .ownershipTeam(request.getOwnershipTeam())
                .ownershipContact(request.getOwnershipContact())
                .metadata(request.getMetadata() != null ? serializeMap(request.getMetadata()) : null)
                .build();

        component = componentRepository.save(component);
        log.info("Component registered with id: {}", component.getId());
        return mapToComponentResponse(component);
    }

    @Transactional(readOnly = true)
    public ComponentResponse getComponent(UUID componentId) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));
        return mapToComponentResponse(component);
    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> getComponentsByProject(UUID projectId) {
        return componentRepository.findByProjectId(projectId).stream()
                .map(this::mapToComponentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> searchComponents(UUID projectId, String search) {
        return componentRepository.searchByName(projectId, search).stream()
                .map(this::mapToComponentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComponentResponse updateComponent(UUID componentId, ComponentRequest request) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));

        if (request.getComponentName() != null) component.setComponentName(request.getComponentName());
        if (request.getComponentPath() != null) component.setComponentPath(request.getComponentPath());
        if (request.getOwnershipTeam() != null) component.setOwnershipTeam(request.getOwnershipTeam());
        if (request.getOwnershipContact() != null) component.setOwnershipContact(request.getOwnershipContact());
        if (request.getMetadata() != null) component.setMetadata(serializeMap(request.getMetadata()));

        component = componentRepository.save(component);
        return mapToComponentResponse(component);
    }

    @Transactional
    public void deleteComponent(UUID componentId) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Component", "id", componentId));
        componentRepository.delete(component);
        log.info("Component deleted: {}", componentId);
    }

    // ==================== Test-Component Mapping ====================

    @Transactional
    public void mapTestToComponent(TestComponentMappingRequest request) {
        Component component = componentRepository.findById(request.getComponentId())
                .orElseThrow(() -> new ResourceNotFoundException("Component", "id", request.getComponentId()));

        TestIssue test = testIssueRepository.findById(request.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", request.getTestId()));

        Optional<TestComponentMapping> existing = testComponentMappingRepository.findByTestIdAndComponentId(
                request.getTestId(), request.getComponentId());

        if (existing.isPresent()) {
            TestComponentMapping mapping = existing.get();
            if (request.getConfidenceScore() != null) mapping.setConfidenceScore(request.getConfidenceScore());
            if (request.getMappingType() != null) mapping.setMappingType(request.getMappingType());
            testComponentMappingRepository.save(mapping);
        } else {
            TestComponentMapping mapping = TestComponentMapping.builder()
                    .testId(request.getTestId())
                    .componentId(request.getComponentId())
                    .confidenceScore(request.getConfidenceScore() != null ? request.getConfidenceScore() : BigDecimal.ONE)
                    .mappingType(request.getMappingType() != null ? request.getMappingType() : "direct")
                    .build();
            testComponentMappingRepository.save(mapping);
        }

        log.info("Mapped test {} to component {}", request.getTestId(), request.getComponentId());
    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> getComponentsForTest(UUID testId) {
        List<UUID> componentIds = testComponentMappingRepository.findComponentIdsByTestId(testId);
        return componentRepository.findAllById(componentIds).stream()
                .map(this::mapToComponentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestImpactDto> getTestsForComponent(UUID componentId) {
        List<UUID> testIds = testComponentMappingRepository.findTestIdsByComponentId(componentId);
        List<TestIssue> tests = testIssueRepository.findAllById(testIds);

        return tests.stream().map(test -> TestImpactDto.builder()
                .testId(test.getId())
                .testIssueKey(test.getName())
                .testName(test.getName())
                .impactLevel("HIGH")
                .riskScore(BigDecimal.ONE)
                .reason("Direct component mapping")
                .build()
        ).collect(Collectors.toList());
    }

    // ==================== Impact Analysis ====================

    @Transactional
    public ImpactAnalysisResponse analyzeImpact(ImpactAnalysisRequest request) {
        log.info("Analyzing impact for project: {} with trigger: {}", request.getProjectId(), request.getTriggerType());

        // Record the code change event
        CodeChangeEvent changeEvent = null;
        if (request.getCommitSha() != null) {
            changeEvent = recordCodeChange(request);
        }

        // Determine affected components based on changed files
        Set<UUID> affectedComponents = determineAffectedComponents(request.getChangedFiles(), request.getProjectId());

        // Find tests mapped to affected components
        List<TestImpactDto> affectedTests = findAffectedTests(affectedComponents);

        // Calculate risk score
        BigDecimal riskScore = calculateRiskScore(affectedTests);

        // Generate suggested suites
        List<String> suggestedSuites = generateSuggestedSuites(affectedTests);

        // Store analysis result
        ImpactAnalysisResult result = ImpactAnalysisResult.builder()
                .projectId(request.getProjectId())
                .triggerType(request.getTriggerType())
                .triggerId(changeEvent != null ? changeEvent.getId() : null)
                .analysisPayload(serializeImpactAnalysis(affectedTests))
                .suggestedSuite(serializeSuites(suggestedSuites))
                .riskScore(riskScore)
                .confidenceScore(calculateConfidence(affectedComponents.size(), affectedTests.size()))
                .analyzedBy("rule-based")
                .build();

        result = impactAnalysisResultRepository.save(result);
        log.info("Impact analysis completed with result id: {}", result.getId());

        return ImpactAnalysisResponse.builder()
                .id(result.getId())
                .projectId(request.getProjectId())
                .triggerType(request.getTriggerType())
                .triggerId(result.getTriggerId())
                .affectedTests(affectedTests)
                .suggestedSuites(suggestedSuites)
                .riskScore(riskScore)
                .confidenceScore(result.getConfidenceScore())
                .analyzedBy(result.getAnalyzedBy())
                .createdAt(result.getCreatedAt())
                .build();
    }

    @Transactional
    public CodeChangeEvent recordCodeChange(ImpactAnalysisRequest request) {
        CodeChangeEvent event = CodeChangeEvent.builder()
                .projectId(request.getProjectId())
                .commitSha(request.getCommitSha())
                .commitMessage(request.getCommitMessage())
                .changedFiles(serializeChangedFiles(request.getChangedFiles()))
                .prId(request.getPrId())
                .branch(request.getBranch())
                .build();

        return codeChangeEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<ImpactAnalysisResponse> getAnalysisHistory(UUID projectId) {
        List<ImpactAnalysisResult> results = impactAnalysisResultRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return results.stream().map(this::mapToAnalysisResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ImpactAnalysisResponse getAnalysisResult(UUID analysisId) {
        ImpactAnalysisResult result = impactAnalysisResultRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("ImpactAnalysisResult", "id", analysisId));
        return mapToAnalysisResponse(result);
    }

    // ==================== Helper Methods ====================

    private Set<UUID> determineAffectedComponents(String[] changedFiles, UUID projectId) {
        Set<UUID> affectedComponents = new HashSet<>();

        if (changedFiles == null || changedFiles.length == 0) {
            return affectedComponents;
        }

        List<Component> components = componentRepository.findByProjectId(projectId);

        for (String file : changedFiles) {
            for (Component component : components) {
                if (component.getComponentPath() != null &&
                    file.contains(component.getComponentPath())) {
                    affectedComponents.add(component.getId());
                }
            }
        }

        return affectedComponents;
    }

    private List<TestImpactDto> findAffectedTests(Set<UUID> componentIds) {
        List<TestImpactDto> affectedTests = new ArrayList<>();

        for (UUID componentId : componentIds) {
            List<UUID> testIds = testComponentMappingRepository.findTestIdsByComponentId(componentId);
            List<TestIssue> tests = testIssueRepository.findAllById(testIds);

            for (TestIssue test : tests) {
                Optional<TestComponentMapping> mapping = testComponentMappingRepository
                        .findByTestIdAndComponentId(test.getId(), componentId);

                BigDecimal confidence = mapping.map(TestComponentMapping::getConfidenceScore).orElse(BigDecimal.ONE);
                String impactLevel = calculateImpactLevel(confidence);
                String reason = mapping.map(m -> "Mapped to component with " + m.getConfidenceScore() + " confidence").orElse("");

                affectedTests.add(TestImpactDto.builder()
                        .testId(test.getId())
                        .testIssueKey(test.getName())
                        .testName(test.getName())
                        .impactLevel(impactLevel)
                        .riskScore(confidence)
                        .reason(reason)
                        .build());
            }
        }

        // Sort by risk score descending
        affectedTests.sort((a, b) -> b.getRiskScore().compareTo(a.getRiskScore()));

        return affectedTests;
    }

    private String calculateImpactLevel(BigDecimal confidenceScore) {
        if (confidenceScore.compareTo(new BigDecimal("0.8")) >= 0) {
            return "HIGH";
        } else if (confidenceScore.compareTo(new BigDecimal("0.5")) >= 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private BigDecimal calculateRiskScore(List<TestImpactDto> affectedTests) {
        if (affectedTests.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long highCount = affectedTests.stream()
                .filter(t -> "HIGH".equals(t.getImpactLevel())).count();
        long mediumCount = affectedTests.stream()
                .filter(t -> "MEDIUM".equals(t.getImpactLevel())).count();

        double score = (highCount * 1.0 + mediumCount * 0.5) / affectedTests.size();
        return BigDecimal.valueOf(Math.min(score * 100, 100));
    }

    private BigDecimal calculateConfidence(int componentCount, int testCount) {
        // More components and tests = more confidence
        double confidence = Math.min((componentCount + testCount) / 20.0, 1.0);
        return BigDecimal.valueOf(confidence);
    }

    private List<String> generateSuggestedSuites(List<TestImpactDto> affectedTests) {
        List<String> suites = new ArrayList<>();

        long highImpact = affectedTests.stream()
                .filter(t -> "HIGH".equals(t.getImpactLevel())).count();

        if (highImpact > 0) {
            suites.add("Critical Regression Suite");
        }
        suites.add("Smoke Test Suite");
        suites.add("Integration Test Suite");

        return suites;
    }

    private String serializeMap(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize map: {}", e.getMessage());
            return "{}";
        }
    }

    private String serializeChangedFiles(String[] files) {
        try {
            return objectMapper.writeValueAsString(files);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String serializeImpactAnalysis(List<TestImpactDto> tests) {
        try {
            return objectMapper.writeValueAsString(tests);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String serializeSuites(List<String> suites) {
        try {
            return objectMapper.writeValueAsString(suites);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private ComponentResponse mapToComponentResponse(Component component) {
        Map<String, Object> metadata = null;
        if (component.getMetadata() != null) {
            try {
                metadata = objectMapper.readValue(component.getMetadata(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse metadata: {}", e.getMessage());
            }
        }

        return ComponentResponse.builder()
                .id(component.getId())
                .projectId(component.getProjectId())
                .componentName(component.getComponentName())
                .componentPath(component.getComponentPath())
                .ownershipTeam(component.getOwnershipTeam())
                .ownershipContact(component.getOwnershipContact())
                .metadata(metadata)
                .createdAt(component.getCreatedAt())
                .build();
    }

    private ImpactAnalysisResponse mapToAnalysisResponse(ImpactAnalysisResult result) {
        List<TestImpactDto> tests = new ArrayList<>();
        List<String> suites = new ArrayList<>();

        if (result.getAnalysisPayload() != null) {
            try {
                tests = objectMapper.readValue(result.getAnalysisPayload(),
                        new TypeReference<List<TestImpactDto>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse analysis payload: {}", e.getMessage());
            }
        }

        if (result.getSuggestedSuite() != null) {
            try {
                suites = objectMapper.readValue(result.getSuggestedSuite(),
                        new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse suggested suites: {}", e.getMessage());
            }
        }

        return ImpactAnalysisResponse.builder()
                .id(result.getId())
                .projectId(result.getProjectId())
                .triggerType(result.getTriggerType())
                .triggerId(result.getTriggerId())
                .affectedTests(tests)
                .suggestedSuites(suites)
                .riskScore(result.getRiskScore())
                .confidenceScore(result.getConfidenceScore())
                .analyzedBy(result.getAnalyzedBy())
                .createdAt(result.getCreatedAt())
                .build();
    }
}