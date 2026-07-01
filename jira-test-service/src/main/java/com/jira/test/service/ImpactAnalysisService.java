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
    private final ImpactGraphRepository impactGraphRepository;
    private final RequirementLinkRepository requirementLinkRepository;

    // ==================== Dependency Graph Building ====================

    @Transactional
    public ImpactGraphDto buildDependencyGraph(UUID projectId, String sourceType, UUID sourceId) {
        log.info("Building dependency graph for {} {} in project {}", sourceType, sourceId, projectId);

        List<ImpactGraph> graphEdges = new ArrayList<>();

        if ("TEST".equals(sourceType)) {
            // Get components mapped to this test
            List<UUID> componentIds = testComponentMappingRepository.findComponentIdsByTestId(sourceId);
            for (UUID componentId : componentIds) {
                ImpactGraph edge = ImpactGraph.builder()
                        .projectId(projectId)
                        .sourceType("TEST")
                        .sourceId(sourceId)
                        .targetType("COMPONENT")
                        .targetId(componentId)
                        .impactType(ImpactGraph.ImpactType.DIRECT)
                        .weight(1.0)
                        .cascadeDepth(0)
                        .build();
                graphEdges.add(impactGraphRepository.save(edge));

                // Find other tests mapped to same component
                List<UUID> relatedTestIds = testComponentMappingRepository.findTestIdsByComponentId(componentId);
                for (UUID relatedTestId : relatedTestIds) {
                    if (!relatedTestId.equals(sourceId)) {
                        ImpactGraph transitiveEdge = ImpactGraph.builder()
                                .projectId(projectId)
                                .sourceType("TEST")
                                .sourceId(sourceId)
                                .targetType("TEST")
                                .targetId(relatedTestId)
                                .impactType(ImpactGraph.ImpactType.TRANSITIVE)
                                .weight(0.8)
                                .cascadeDepth(1)
                                .build();
                        graphEdges.add(impactGraphRepository.save(transitiveEdge));
                    }
                }
            }

            // Get requirements linked to this test
            List<RequirementLink> reqLinks = requirementLinkRepository.findByTestId(sourceId);
            for (RequirementLink reqLink : reqLinks) {
                // Find other tests covering same requirement
                List<RequirementLink> otherReqLinks = requirementLinkRepository.findByRequirementKey(reqLink.getRequirementKey());
                for (RequirementLink otherLink : otherReqLinks) {
                    if (!otherLink.getTestId().equals(sourceId)) {
                        ImpactGraph reqEdge = ImpactGraph.builder()
                                .projectId(projectId)
                                .sourceType("TEST")
                                .sourceId(sourceId)
                                .targetType("TEST")
                                .targetId(otherLink.getTestId())
                                .impactType(ImpactGraph.ImpactType.CASCADING)
                                .weight(0.6)
                                .cascadeDepth(2)
                                .description("Shared requirement: " + reqLink.getRequirementKey())
                                .build();
                        graphEdges.add(impactGraphRepository.save(reqEdge));
                    }
                }
            }
        }

        return mapToGraphDto(graphEdges.isEmpty() ? null : graphEdges.get(0));
    }

    @Transactional(readOnly = true)
    public List<ImpactGraphDto> getDependencyGraph(UUID projectId, UUID sourceId, Integer maxDepth) {
        List<ImpactGraph> edges = impactGraphRepository.findBySourceWithMaxDepth("TEST", sourceId, maxDepth != null ? maxDepth : 3);
        return edges.stream().map(this::mapToGraphDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestImpactDetailDto> getAffectedTests(UUID projectId, String changeType, String changeKey) {
        List<TestImpactDetailDto> affectedTests = new ArrayList<>();

        if ("COMPONENT".equals(changeType)) {
            UUID componentId = UUID.fromString(changeKey);
            List<UUID> testIds = testComponentMappingRepository.findTestIdsByComponentId(componentId);
            for (UUID testId : testIds) {
                TestIssue test = testIssueRepository.findById(testId).orElse(null);
                if (test != null) {
                    TestImpactDetailDto detail = buildTestImpactDetail(test, 0, "Direct component mapping");
                    affectedTests.add(detail);
                }
            }
        } else if ("REQUIREMENT".equals(changeType)) {
            List<RequirementLink> reqLinks = requirementLinkRepository.findByRequirementKey(changeKey);
            for (RequirementLink reqLink : reqLinks) {
                TestIssue test = testIssueRepository.findById(reqLink.getTestId()).orElse(null);
                if (test != null) {
                    TestImpactDetailDto detail = buildTestImpactDetail(test, 0, "Requirement coverage: " + changeKey);
                    detail.setRequirementKey(changeKey);
                    affectedTests.add(detail);
                }
            }
        }

        return affectedTests;
    }

    // ==================== Cascading Impact Calculation ====================

    @Transactional(readOnly = true)
    public TestImpactDetailDto analyzeTestImpact(UUID testId, Integer cascadeDepth) {
        log.info("Analyzing impact for test {} with cascade depth {}", testId, cascadeDepth);

        TestIssue test = testIssueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        TestImpactDetailDto detail = buildTestImpactDetail(test, 0, "Source test");
        detail.setMitigationSuggestions(generateMitigationSuggestions(test));

        // Calculate cascade impact
        int maxDepth = cascadeDepth != null ? cascadeDepth : 3;
        Set<UUID> visited = new HashSet<>();
        visited.add(testId);
        List<TestImpactDetailDto> allAffected = new ArrayList<>();
        allAffected.add(detail);

        calculateCascadeImpact(testId, 1, maxDepth, visited, allAffected);

        // Store dependent tests
        List<String> dependentTestKeys = allAffected.stream()
                .skip(1)
                .map(TestImpactDetailDto::getTestIssueKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        detail.setDependentTests(dependentTestKeys);

        return detail;
    }

    private void calculateCascadeImpact(UUID sourceTestId, int currentDepth, int maxDepth,
                                         Set<UUID> visited, List<TestImpactDetailDto> allAffected) {
        if (currentDepth > maxDepth) return;

        // Get components for this test
        List<UUID> componentIds = testComponentMappingRepository.findComponentIdsByTestId(sourceTestId);

        for (UUID componentId : componentIds) {
            List<UUID> relatedTestIds = testComponentMappingRepository.findTestIdsByComponentId(componentId);

            for (UUID relatedTestId : relatedTestIds) {
                if (!visited.contains(relatedTestId)) {
                    visited.add(relatedTestId);
                    TestIssue relatedTest = testIssueRepository.findById(relatedTestId).orElse(null);
                    if (relatedTest != null) {
                        TestImpactDetailDto cascadeDetail = buildTestImpactDetail(
                                relatedTest, currentDepth,
                                "Cascade level " + currentDepth + " via component"
                        );
                        cascadeDetail.setRiskScore(cascadeDetail.getRiskScore() * (1.0 / currentDepth));
                        allAffected.add(cascadeDetail);

                        // Recurse for next level
                        calculateCascadeImpact(relatedTestId, currentDepth + 1, maxDepth, visited, allAffected);
                    }
                }
            }
        }

        // Check requirement-based cascade
        List<RequirementLink> reqLinks = requirementLinkRepository.findByTestId(sourceTestId);
        for (RequirementLink reqLink : reqLinks) {
            List<RequirementLink> otherReqLinks = requirementLinkRepository.findByRequirementKey(reqLink.getRequirementKey());
            for (RequirementLink otherLink : otherReqLinks) {
                if (!visited.contains(otherLink.getTestId())) {
                    visited.add(otherLink.getTestId());
                    TestIssue relatedTest = testIssueRepository.findById(otherLink.getTestId()).orElse(null);
                    if (relatedTest != null) {
                        TestImpactDetailDto cascadeDetail = buildTestImpactDetail(
                                relatedTest, currentDepth,
                                "Requirement cascade: " + reqLink.getRequirementKey()
                        );
                        cascadeDetail.setRiskScore(cascadeDetail.getRiskScore() * 0.5);
                        cascadeDetail.setRequirementKey(reqLink.getRequirementKey());
                        allAffected.add(cascadeDetail);
                    }
                }
            }
        }
    }

    // ==================== Risk Scoring Algorithm ====================

    public String calculateRiskLevel(double riskScore) {
        if (riskScore >= 80) return "CRITICAL";
        if (riskScore >= 60) return "HIGH";
        if (riskScore >= 30) return "MEDIUM";
        return "LOW";
    }

    @Transactional(readOnly = true)
    public Double calculateComprehensiveRiskScore(UUID projectId, List<UUID> testIds) {
        double totalRisk = 0.0;
        int count = 0;

        for (UUID testId : testIds) {
            // Base risk from test characteristics
            TestIssue test = testIssueRepository.findById(testId).orElse(null);
            if (test != null) {
                double testRisk = 1.0;

                // Higher risk for automated tests
                if ("AUTOMATED".equals(test.getTestType())) testRisk *= 1.2;

                // Higher risk for tests with recent failures
                // (would need execution history - simplified for now)

                // Risk from cascade depth
                int componentCount = testComponentMappingRepository.findComponentIdsByTestId(testId).size();
                testRisk *= (1 + componentCount * 0.1);

                totalRisk += testRisk;
                count++;
            }
        }

        return count > 0 ? (totalRisk / count) * 100 : 0.0;
    }

    // ==================== Mitigation Suggestions ====================

    public List<String> generateMitigationSuggestions(TestIssue test) {
        List<String> suggestions = new ArrayList<>();

        // Analyze test characteristics and suggest appropriate mitigation
        if ("AUTOMATED".equals(test.getTestType())) {
            suggestions.add("Run automated regression suite before deployment");
            suggestions.add("Enable CI/CD pipeline gates for this test");
        } else {
            suggestions.add("Schedule manual test execution in next test cycle");
            suggestions.add("Consider automating this test for faster feedback");
        }

        // Check test priority
        if ("HIGH".equals(test.getPriority()) || "CRITICAL".equals(test.getPriority())) {
            suggestions.add("Prioritize this test in the execution order");
            suggestions.add("Execute in parallel with related tests if possible");
        }

        // Check component dependencies
        int componentCount = testComponentMappingRepository.findComponentIdsByTestId(test.getId()).size();
        if (componentCount > 3) {
            suggestions.add("Consider breaking down test into smaller focused tests");
            suggestions.add("Review component boundaries for better test isolation");
        }

        // Check for shared steps usage
        suggestions.add("Review shared steps that this test depends on");
        suggestions.add("Verify test data and datasets are up to date");

        // General suggestions
        suggestions.add("Document any environment-specific requirements");
        suggestions.add("Check for flaky test patterns in recent executions");

        return suggestions;
    }

    // ==================== Requirement Impact Analysis ====================

    @Transactional(readOnly = true)
    public RequirementImpactDto analyzeRequirementImpact(String requirementKey, Integer fromVersion, Integer toVersion) {
        log.info("Analyzing requirement impact for {} from v{} to v{}", requirementKey, fromVersion, toVersion);

        List<TestImpactDetailDto> affectedTests = new ArrayList<>();
        List<RequirementLink> reqLinks = requirementLinkRepository.findByRequirementKey(requirementKey);

        for (RequirementLink reqLink : reqLinks) {
            TestIssue test = testIssueRepository.findById(reqLink.getTestId()).orElse(null);
            if (test != null) {
                TestImpactDetailDto detail = buildTestImpactDetail(test, 0, "Requirement coverage");
                detail.setRequirementKey(requirementKey);
                detail.setMitigationSuggestions(generateMitigationSuggestions(test));
                affectedTests.add(detail);
            }
        }

        double avgRisk = affectedTests.stream()
                .mapToDouble(t -> t.getRiskScore() != null ? t.getRiskScore() : 0.0)
                .average()
                .orElse(0.0);

        String riskLevel = calculateRiskLevel(avgRisk);
        List<String> suggestedActions = buildSuggestedActions(affectedTests.size(), riskLevel);

        return RequirementImpactDto.builder()
                .requirementKey(requirementKey)
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .changeType("MODIFIED")
                .affectedTestsCount(affectedTests.size())
                .affectedTests(affectedTests)
                .riskLevel(riskLevel)
                .suggestedActions(suggestedActions)
                .build();
    }

    // ==================== Batch Impact Analysis ====================

    @Transactional
    public BatchImpactAnalysisResponse analyzeBatchImpact(BatchImpactAnalysisRequest request) {
        log.info("Batch impact analysis for {} tests in project {}", request.getTestIds().size(), request.getProjectId());

        List<TestImpactDetailDto> allAffected = new ArrayList<>();
        List<ImpactGraphDto> graphData = new ArrayList<>();
        Set<UUID> allTestIds = new HashSet<>(request.getTestIds());

        for (UUID testId : request.getTestIds()) {
            TestImpactDetailDto detail = analyzeTestImpact(testId, request.getCascadeDepth());
            allAffected.add(detail);

            // Collect graph data
            List<ImpactGraph> edges = impactGraphRepository.findBySourceWithMaxDepth(
                    "TEST", testId, request.getCascadeDepth() != null ? request.getCascadeDepth() : 3);
            for (ImpactGraph edge : edges) {
                graphData.add(mapToGraphDto(edge));
            }
        }

        // Calculate overall risk
        List<UUID> testIdList = allAffected.stream()
                .map(t -> UUID.fromString(t.getTestId()))
                .collect(Collectors.toList());
        Double overallRisk = calculateComprehensiveRiskScore(request.getProjectId(), testIdList);
        String riskLevel = calculateRiskLevel(overallRisk);

        // Generate summary
        List<String> suggestedSuites = generateSuggestedSuitesForBatch(allAffected);
        List<String> mitigationSummary = buildBatchMitigationSummary(allAffected);

        return BatchImpactAnalysisResponse.builder()
                .totalAnalyzed(request.getTestIds().size())
                .totalAffected(allAffected.size())
                .overallRiskScore(overallRisk)
                .riskLevel(riskLevel)
                .allAffectedTests(allAffected)
                .graphData(graphData)
                .suggestedSuites(suggestedSuites)
                .mitigationSummary(mitigationSummary)
                .build();
    }

    // ==================== Helper Methods ====================

    private TestImpactDetailDto buildTestImpactDetail(TestIssue test, int cascadeLevel, String reason) {
        // Get component info
        List<UUID> componentIds = testComponentMappingRepository.findComponentIdsByTestId(test.getId());
        String componentName = componentIds.isEmpty() ? null :
                componentRepository.findById(componentIds.get(0))
                        .map(Component::getComponentName)
                        .orElse(null);

        // Get requirement keys
        List<String> reqKeys = requirementLinkRepository.findByTestId(test.getId())
                .stream()
                .map(RequirementLink::getRequirementKey)
                .collect(Collectors.toList());

        // Calculate risk score
        double riskScore = calculateTestRiskScore(test, cascadeLevel);

        return TestImpactDetailDto.builder()
                .testId(test.getId().toString())
                .testIssueKey(test.getName())
                .testName(test.getName())
                .testType(test.getTestType())
                .status(test.getStatus())
                .impactLevel(determineImpactLevel(riskScore))
                .riskScore(riskScore)
                .reason(reason)
                .cascadeLevel(cascadeLevel)
                .componentName(componentName)
                .requirementKey(reqKeys.isEmpty() ? null : reqKeys.get(0))
                .build();
    }

    private double calculateTestRiskScore(TestIssue test, int cascadeLevel) {
        double baseScore = 50.0;

        // Test type factor
        if ("AUTOMATED".equals(test.getTestType())) baseScore += 20;
        else if ("BDD".equals(test.getTestType())) baseScore += 15;

        // Status factor
        if ("APPROVED".equals(test.getStatus())) baseScore -= 10;
        else if ("DRAFT".equals(test.getStatus())) baseScore += 15;

        // Cascade depth factor (decreasing impact)
        baseScore *= (1.0 / (cascadeLevel + 1));

        return Math.min(Math.max(baseScore, 0), 100);
    }

    private String determineImpactLevel(double riskScore) {
        if (riskScore >= 75) return "CRITICAL";
        if (riskScore >= 50) return "HIGH";
        if (riskScore >= 25) return "MEDIUM";
        return "LOW";
    }

    private ImpactGraphDto mapToGraphDto(ImpactGraph graph) {
        if (graph == null) return null;

        String sourceLabel = getEntityLabel(graph.getSourceType(), graph.getSourceId());
        String targetLabel = getEntityLabel(graph.getTargetType(), graph.getTargetId());

        return ImpactGraphDto.builder()
                .id(graph.getId())
                .sourceType(graph.getSourceType())
                .sourceId(graph.getSourceId())
                .sourceLabel(sourceLabel)
                .targetType(graph.getTargetType())
                .targetId(graph.getTargetId())
                .targetLabel(targetLabel)
                .impactType(graph.getImpactType().name())
                .weight(graph.getWeight())
                .cascadeDepth(graph.getCascadeDepth())
                .build();
    }

    private String getEntityLabel(String type, UUID id) {
        if (id == null) return "Unknown";
        switch (type) {
            case "TEST":
                return testIssueRepository.findById(id)
                        .map(t -> t.getName())
                        .orElse("Test-" + id.toString().substring(0, 8));
            case "COMPONENT":
                return componentRepository.findById(id)
                        .map(c -> c.getComponentName())
                        .orElse("Component-" + id.toString().substring(0, 8));
            default:
                return type + "-" + id.toString().substring(0, 8);
        }
    }

    private List<String> buildSuggestedActions(int affectedCount, String riskLevel) {
        List<String> actions = new ArrayList<>();

        if ("CRITICAL".equals(riskLevel) || "HIGH".equals(riskLevel)) {
            actions.add("Run full regression suite immediately");
            actions.add("Block deployment until tests pass");
        }

        if (affectedCount > 10) {
            actions.add("Prioritize critical path tests");
            actions.add("Consider parallel test execution");
        }

        actions.add("Review change impact with development team");
        actions.add("Update test documentation if needed");

        return actions;
    }

    private List<String> generateSuggestedSuitesForBatch(List<TestImpactDetailDto> affectedTests) {
        List<String> suites = new ArrayList<>();

        long criticalCount = affectedTests.stream()
                .filter(t -> "CRITICAL".equals(t.getImpactLevel()))
                .count();

        long highCount = affectedTests.stream()
                .filter(t -> "HIGH".equals(t.getImpactLevel()))
                .count();

        if (criticalCount > 0) {
            suites.add("Critical Regression Suite");
        }
        if (highCount > 0 || criticalCount > 0) {
            suites.add("High Priority Test Suite");
        }
        suites.add("Smoke Test Suite");
        suites.add("Integration Test Suite");

        return suites;
    }

    private List<String> buildBatchMitigationSummary(List<TestImpactDetailDto> affectedTests) {
        List<String> summary = new ArrayList<>();

        Map<String, Long> byType = affectedTests.stream()
                .collect(Collectors.groupingBy(t -> t.getTestType() != null ? t.getTestType() : "UNKNOWN", Collectors.counting()));

        summary.add("Total affected tests: " + affectedTests.size());
        summary.add("By test type: " + byType.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ")));

        long automated = affectedTests.stream()
                .filter(t -> "AUTOMATED".equals(t.getTestType()))
                .count();
        if (automated > 0) {
            summary.add("Automated tests can be run in CI/CD pipeline");
        }

        long manual = affectedTests.stream()
                .filter(t -> "MANUAL".equals(t.getTestType()))
                .count();
        if (manual > 0) {
            summary.add("Manual tests require scheduling in test cycle");
        }

        return summary;
    }

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
                .testId(test.getId().toString())
                .testIssueKey(test.getName())
                .testName(test.getName())
                .impactLevel("HIGH")
                .riskScore(1.0)
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
                        .testId(test.getId().toString())
                        .testIssueKey(test.getName())
                        .testName(test.getName())
                        .impactLevel(impactLevel)
                        .riskScore(confidence.doubleValue())
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