package com.jira.issue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.issue.dto.*;
import com.jira.issue.entity.*;
import com.jira.issue.exception.*;
import com.jira.issue.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TestManagementService - Native test management as first-class Jira issues
 * Tests are stored as issues with issue_type = 'Test'
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestManagementService {

    private final IssueRepository issueRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final ProjectRepository projectRepository;
    private final TestRepositoryFolderRepository folderRepository;
    private final TestSetRepository testSetRepository;
    private final TestPlanRepository testPlanRepository;
    private final TestPlanItemRepository testPlanItemRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;
    private final RequirementLinkRepository requirementLinkRepository;
    private final DefectLinkRepository defectLinkRepository;
    private final TestEnvironmentRepository environmentRepository;
    private final TestDatasetRepository datasetRepository;
    private final TestExecutionHistoryRepository historyRepository;
    private final TestEvidenceRepository evidenceRepository;
    private final SharedStepRepository sharedStepRepository;
    private final SharedStepUsageRepository sharedStepUsageRepository;
    private final TestVersionRepository versionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.defaults.test-type:MANUAL}")
    private String defaultTestType;

    @Value("${app.defaults.test-status:DRAFT}")
    private String defaultTestStatus;

    @Value("${app.defaults.test-plan-status:OPEN}")
    private String defaultTestPlanStatus;

    @Value("${app.defaults.test-set-status:DRAFT}")
    private String defaultTestSetStatus;

    @Value("${app.defaults.execution-status:RUNNING}")
    private String defaultExecutionStatus;

    @Value("${app.defaults.coverage-status:COVERED}")
    private String defaultCoverageStatus;

    @Value("${app.defaults.defect-status:OPEN}")
    private String defaultDefectStatus;

    // Helper methods to get project key and default status
    private String getProjectKey(UUID projectId) {
        return projectRepository.findById(projectId)
                .map(p -> p.getProjectKey())
                .orElse(null);
    }

    private IssueStatus getDefaultStatus() {
        return issueStatusRepository.findByName("To Do")
                .orElse(issueStatusRepository.findAll().stream().findFirst().orElse(null));
    }

    private String generateIssueKey(String projectKey) {
        if (projectKey == null) return "TEST-1";
        String normalizedKey = projectKey.substring(0, Math.min(projectKey.length(), 6)).toUpperCase();
        Long count = issueRepository.count() + 1;
        return normalizedKey + "-" + count;
    }

    // ==================== Test CRUD ====================

    @Transactional
    public TestResponse createTest(UUID projectId, CreateTestRequest request, UUID userId) {
        log.info("Creating test: {} for project: {}", request.getName(), projectId);

        // Validate project
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Get Test issue type
        IssueType testType = issueTypeRepository.findByName("Test")
                .orElseThrow(() -> new ValidationException("Test issue type not found. Please seed test issue type."));

        // Build test steps as JSON
        String testStepsJson = buildTestStepsJson(request.getSteps());

        // Create the test issue
        String projectKey = getProjectKey(projectId);
        String issueKey = generateIssueKey(projectKey);

        Issue testIssue = Issue.builder()
                .projectId(projectId)
                .issueKey(issueKey)
                .title(request.getName())
                .description(request.getDescription())
                .issueType(testType)
                .status(getDefaultStatus())
                .testType(request.getTestType() != null ? request.getTestType() : defaultTestType)
                .testStatus(request.getTestStatus() != null ? request.getTestStatus() : defaultTestStatus)
                .testPriority(request.getPriority())
                .testOwnerId(request.getOwnerId())
                .testSteps(testStepsJson)
                .labels(request.getLabels() != null ? request.getLabels().toArray(new String[0]) : new String[]{})
                .requirementKeys(request.getRequirementKeys() != null ? request.getRequirementKeys().toArray(new String[0]) : new String[]{})
                .gherkinFeatureKey(request.getGherkinFeatureKey())
                .gherkinScenarioId(request.getGherkinScenarioId())
                .testSetId(request.getTestSetId())
                .testRepositoryFolderId(request.getFolderId())
                .reporterId(userId)
                .creatorId(userId)
                .build();

        testIssue = issueRepository.save(testIssue);
        log.info("Test created with id: {}", testIssue.getId());

        return mapToTestResponse(testIssue);
    }

    @Transactional(readOnly = true)
    public TestResponse getTest(UUID testId) {
        Issue test = issueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
        return mapToTestResponse(test);
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getTestsByProject(UUID projectId, String testType, String status, UUID folderId) {
        List<Issue> tests;

        if (folderId != null) {
            tests = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test").stream()
                    .filter(t -> folderId.equals(t.getTestRepositoryFolderId()))
                    .collect(Collectors.toList());
        } else if (testType != null) {
            tests = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test").stream()
                    .filter(t -> testType.equals(t.getTestType()))
                    .collect(Collectors.toList());
        } else if (status != null) {
            tests = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test").stream()
                    .filter(t -> status.equals(t.getTestStatus()))
                    .collect(Collectors.toList());
        } else {
            tests = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test");
        }

        return tests.stream().map(this::mapToTestResponse).collect(Collectors.toList());
    }

    @Transactional
    public TestResponse updateTest(UUID testId, CreateTestRequest request) {
        Issue test = issueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        if (request.getName() != null) test.setTitle(request.getName());
        if (request.getDescription() != null) test.setDescription(request.getDescription());
        if (request.getTestType() != null) test.setTestType(request.getTestType());
        if (request.getTestStatus() != null) test.setTestStatus(request.getTestStatus());
        if (request.getPriority() != null) test.setTestPriority(request.getPriority());
        if (request.getOwnerId() != null) test.setTestOwnerId(request.getOwnerId());
        if (request.getLabels() != null) test.setLabels(request.getLabels().toArray(new String[0]));
        if (request.getRequirementKeys() != null) test.setRequirementKeys(request.getRequirementKeys().toArray(new String[0]));
        if (request.getSteps() != null) test.setTestSteps(buildTestStepsJson(request.getSteps()));
        if (request.getTestSetId() != null) test.setTestSetId(request.getTestSetId());
        if (request.getFolderId() != null) test.setTestRepositoryFolderId(request.getFolderId());

        test = issueRepository.save(test);
        return mapToTestResponse(test);
    }

    @Transactional
    public void deleteTest(UUID testId) {
        Issue test = issueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
        test.setArchived(true);
        issueRepository.save(test);
        log.info("Test archived: {}", testId);
    }

    // ==================== Test Repository Folders ====================

    @Transactional
    public TestRepositoryFolderResponse createFolder(UUID projectId, CreateFolderRequest request, UUID userId) {
        TestRepositoryFolder folder = TestRepositoryFolder.builder()
                .projectId(projectId)
                .parentFolderId(request.getParentFolderId())
                .name(request.getName())
                .description(request.getDescription())
                .path(buildFolderPath(request.getParentFolderId(), request.getName()))
                .depth(calculateFolderDepth(request.getParentFolderId()))
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isSmartFolder(request.getIsSmartFolder() != null ? request.getIsSmartFolder() : false)
                .smartFolderQuery(request.getSmartFolderQuery())
                .createdBy(userId)
                .build();

        folder = folderRepository.save(folder);
        log.info("Folder created: {}", folder.getId());
        return mapToFolderResponse(folder);
    }

    @Transactional(readOnly = true)
    public List<TestRepositoryFolderResponse> getFoldersByProject(UUID projectId) {
        return folderRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void moveTestToFolder(UUID testId, UUID folderId) {
        Issue test = issueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
        test.setTestRepositoryFolderId(folderId);
        issueRepository.save(test);
    }

    // ==================== Test Sets ====================

    @Transactional
    public TestSetResponse createTestSet(UUID projectId, CreateTestSetRequest request, UUID userId) {
        log.info("Creating test set: {} for project: {}", request.getName(), projectId);

        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        TestSet testSet = TestSet.builder()
                .projectId(projectId)
                .folderId(request.getFolderId())
                .name(request.getName())
                .description(request.getDescription())
                .testType(request.getTestType() != null ? request.getTestType() : defaultTestType)
                .labels(request.getLabels() != null ? request.getLabels().toArray(new String[0]) : new String[]{})
                .requirementKeys(request.getRequirementKeys() != null ? request.getRequirementKeys().toArray(new String[0]) : new String[]{})
                .status(defaultTestSetStatus)
                .ownerId(request.getOwnerId())
                .createdBy(userId)
                .build();

        testSet = testSetRepository.save(testSet);
        log.info("Test set created: {}", testSet.getId());
        return mapToTestSetResponse(testSet);
    }

    @Transactional(readOnly = true)
    public TestSetResponse getTestSet(UUID testSetId) {
        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", testSetId));
        return mapToTestSetResponse(testSet);
    }

    @Transactional(readOnly = true)
    public List<TestSetResponse> getTestSetsByProject(UUID projectId) {
        return testSetRepository.findByProjectIdAndArchivedFalseOrderByNameAsc(projectId).stream()
                .map(this::mapToTestSetResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestSetResponse addTestToSet(UUID testSetId, UUID testId) {
        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", testSetId));
        Issue test = issueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        test.setTestSetId(testSetId);
        testSet.setTestCount(testSet.getTestCount() + 1);

        issueRepository.save(test);
        testSetRepository.save(testSet);

        return mapToTestSetResponse(testSet);
    }

    // ==================== Test Plans ====================

    @Transactional
    public TestPlanResponse createTestPlan(UUID projectId, CreateTestPlanRequest request, UUID userId) {
        log.info("Creating test plan: {} for project: {}", request.getName(), projectId);

        TestPlan plan = TestPlan.builder()
                .projectId(projectId)
                .name(request.getName())
                .description(request.getDescription())
                .testType(request.getTestType() != null ? request.getTestType() : defaultTestType)
                .labels(request.getLabels() != null ? request.getLabels().toArray(new String[0]) : new String[]{})
                .status(defaultTestPlanStatus)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .targetVersion(request.getTargetVersion())
                .environment(request.getEnvironment())
                .ownerId(request.getOwnerId())
                .createdBy(userId)
                .build();

        plan = testPlanRepository.save(plan);
        log.info("Test plan created: {}", plan.getId());
        return mapToTestPlanResponse(plan);
    }

    @Transactional
    public TestPlanResponse addTestSetToPlan(UUID planId, UUID testSetId) {
        TestPlan plan = testPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("TestPlan", "id", planId));
        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", testSetId));

        if (testPlanItemRepository.findByTestPlanIdAndTestSetId(planId, testSetId).isPresent()) {
            throw new DuplicateResourceException("Test set already in plan");
        }

        Integer maxOrder = testPlanItemRepository.findMaxExecutionOrder(planId);
        TestPlanItem item = TestPlanItem.builder()
                .testPlanId(planId)
                .testSetId(testSetId)
                .executionOrder(maxOrder != null ? maxOrder + 1 : 1)
                .build();
        testPlanItemRepository.save(item);

        return mapToTestPlanResponse(plan);
    }

    @Transactional(readOnly = true)
    public TestPlanResponse getTestPlan(UUID planId) {
        TestPlan plan = testPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("TestPlan", "id", planId));
        return mapToTestPlanResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<TestPlanResponse> getTestPlansByProject(UUID projectId) {
        return testPlanRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToTestPlanResponse)
                .collect(Collectors.toList());
    }

    // ==================== Test Executions ====================

    @Transactional(readOnly = true)
    public TestExecutionResponse getExecution(UUID executionId) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));
        return mapToExecutionResponse(execution);
    }

    @Transactional(readOnly = true)
    public List<TestExecutionResponse> getExecutionsByProject(UUID projectId) {
        return executionRepository.findByProjectIdOrderByStartedAtDesc(projectId).stream()
                .map(this::mapToExecutionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StepResultResponse> getStepResultsByExecution(UUID executionId) {
        return stepResultRepository.findByExecutionIdOrderByStepOrderAsc(executionId).stream()
                .map(this::mapToStepResultResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestExecutionResponse startExecution(UUID projectId, CreateExecutionRequest request, UUID userId) {
        log.info("Starting test execution: {}", request.getName());

        TestExecution execution = TestExecution.builder()
                .projectId(projectId)
                .testPlanId(request.getTestPlanId())
                .testSetId(request.getTestSetId())
                .testId(request.getTestId())
                .name(request.getName())
                .description(request.getDescription())
                .status(defaultExecutionStatus)
                .testEnv(request.getTestEnv())
                .testerId(userId)
                .testCycle(request.getTestCycle())
                .sprintId(request.getSprintId())
                .ciBuildUrl(request.getCiBuildUrl())
                .ciJobName(request.getCiJobName())
                .ciBuildNumber(request.getCiBuildNumber())
                .ciJobId(request.getCiJobId())
                .branch(request.getBranch())
                .commitSha(request.getCommitSha())
                .startedAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        // Count tests to execute
        int totalTests = countTestsForExecution(request);
        execution.setTotalTests(totalTests);

        execution = executionRepository.save(execution);
        log.info("Execution started: {}", execution.getId());

        return mapToExecutionResponse(execution);
    }

    @Transactional
    public StepResultResponse recordStepResult(UUID executionId, UUID testId, Integer stepOrder, StepResultUpdateRequest request) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));

        StepResult result = stepResultRepository.findByExecutionIdAndIssueIdAndStepOrder(executionId, testId, stepOrder)
                .orElse(StepResult.builder()
                        .executionId(executionId)
                        .issueId(testId)
                        .stepOrder(stepOrder)
                        .stepType(request.getStepType())
                        .stepDescription(request.getStepDescription())
                        .expectedResult(request.getExpectedResult())
                        .build());

        result.setStatus(request.getStatus());
        result.setActualResult(request.getActualResult());
        result.setExecutedAt(LocalDateTime.now());
        result.setExecutionTimeMs(request.getExecutionTimeMs());
        result.setDefectKey(request.getDefectKey());
        result.setDefectSeverity(request.getDefectSeverity());
        result.setComment(request.getComment());
        if (request.getEvidenceIds() != null) result.setEvidenceIds(request.getEvidenceIds().toArray(new UUID[0]));

        result = stepResultRepository.save(result);

        // Update execution summary
        updateExecutionSummary(executionId);

        return mapToStepResultResponse(result);
    }

    @Transactional
    public TestExecutionResponse completeExecution(UUID executionId) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));

        execution.setStatus(calculateExecutionStatus(executionId));
        execution.setFinishedAt(LocalDateTime.now());

        if (execution.getStartedAt() != null) {
            long duration = java.time.Duration.between(execution.getStartedAt(), execution.getFinishedAt()).getSeconds();
            execution.setDurationSeconds(duration);
        }

        execution = executionRepository.save(execution);

        // Record in history
        recordExecutionHistory(execution);

        log.info("Execution completed: {} with status: {}", executionId, execution.getStatus());
        return mapToExecutionResponse(execution);
    }

    // ==================== Traceability ====================

    @Transactional
    public RequirementLinkResponse linkRequirement(RequirementLinkRequest request, UUID userId) {
        Issue test = issueRepository.findById(request.getTestIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", request.getTestIssueId()));

        if (requirementLinkRepository.findByRequirementKeyAndTestIssueId(request.getRequirementKey(), request.getTestIssueId()).isPresent()) {
            throw new DuplicateResourceException("Requirement already linked to this test");
        }

        RequirementLink link = RequirementLink.builder()
                .requirementKey(request.getRequirementKey())
                .requirementSummary(request.getRequirementSummary())
                .requirementType(request.getRequirementType())
                .testIssueId(request.getTestIssueId())
                .coverageStatus(request.getCoverageStatus() != null ? request.getCoverageStatus() : defaultCoverageStatus)
                .createdBy(userId)
                .build();

        link = requirementLinkRepository.save(link);

        // Also update requirement_keys on the test issue
        String[] currentKeys = test.getRequirementKeys();
        List<String> keysList = currentKeys != null ? new ArrayList<>(Arrays.asList(currentKeys)) : new ArrayList<>();
        if (!keysList.contains(request.getRequirementKey())) {
            keysList.add(request.getRequirementKey());
            test.setRequirementKeys(keysList.toArray(new String[0]));
            issueRepository.save(test);
        }

        return mapToRequirementLinkResponse(link);
    }

    @Transactional
    public DefectLinkResponse linkDefect(DefectLinkRequest request, UUID userId) {
        DefectLink link = DefectLink.builder()
                .defectKey(request.getDefectKey())
                .defectSummary(request.getDefectSummary())
                .defectType(request.getDefectType())
                .testExecutionId(request.getTestExecutionId())
                .stepResultId(request.getStepResultId())
                .testIssueId(request.getTestIssueId())
                .severity(request.getSeverity())
                .status(defaultDefectStatus)
                .priority(request.getPriority())
                .linkedBy(userId)
                .build();

        link = defectLinkRepository.save(link);
        return mapToDefectLinkResponse(link);
    }

    @Transactional(readOnly = true)
    public TraceabilityMatrixResponse getTraceabilityMatrix(UUID projectId) {
        // Get all tests for project
        List<Issue> tests = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test");

        // Get all requirement links
        List<String> requirementKeys = tests.stream()
                .map(Issue::getRequirementKeys)
                .filter(k -> k != null)
                .flatMap(Arrays::stream)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Build matrix
        List<TraceabilityMatrixResponse.RequirementRow> rows = new ArrayList<>();
        for (String reqKey : requirementKeys) {
            List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(reqKey);

            List<TraceabilityMatrixResponse.TestCoverage> coverages = links.stream()
                    .map(link -> {
                        Issue test = tests.stream().filter(t -> t.getId().equals(link.getTestIssueId())).findFirst().orElse(null);
                        return TraceabilityMatrixResponse.TestCoverage.builder()
                                .testId(link.getTestIssueId())
                                .testName(test != null ? test.getTitle() : null)
                                .status(link.getLastExecutionStatus())
                                .coverageStatus(link.getCoverageStatus())
                                .build();
                    })
                    .collect(Collectors.toList());

            rows.add(TraceabilityMatrixResponse.RequirementRow.builder()
                    .requirementKey(reqKey)
                    .testCount(coverages.size())
                    .coverageStatus(calculateCoverageStatus(coverages))
                    .tests(coverages)
                    .build());
        }

        return TraceabilityMatrixResponse.builder()
                .projectId(projectId)
                .totalRequirements(rows.size())
                .totalTests(tests.size())
                .overallCoverage(calculateOverallCoverage(rows))
                .requirements(rows)
                .build();
    }

    // ==================== Test Environments ====================

    @Transactional(readOnly = true)
    public TestEnvironmentResponse getEnvironment(UUID envId) {
        TestEnvironment env = environmentRepository.findById(envId)
                .orElseThrow(() -> new ResourceNotFoundException("TestEnvironment", "id", envId));
        return mapToEnvironmentResponse(env);
    }

    @Transactional
    public TestEnvironmentResponse updateEnvironment(UUID envId, CreateEnvironmentRequest request) {
        TestEnvironment env = environmentRepository.findById(envId)
                .orElseThrow(() -> new ResourceNotFoundException("TestEnvironment", "id", envId));

        if (request.getName() != null) env.setName(request.getName());
        if (request.getDescription() != null) env.setDescription(request.getDescription());
        if (request.getEnvironmentType() != null) env.setEnvironmentType(request.getEnvironmentType());
        if (request.getConfig() != null) env.setConfig(request.getConfig());
        if (request.getUrl() != null) env.setUrl(request.getUrl());
        if (request.getVariables() != null) env.setVariables(request.getVariables());
        if (request.getSortOrder() != null) env.setSortOrder(request.getSortOrder());

        env = environmentRepository.save(env);
        return mapToEnvironmentResponse(env);
    }

    @Transactional
    public void deleteEnvironment(UUID envId) {
        TestEnvironment env = environmentRepository.findById(envId)
                .orElseThrow(() -> new ResourceNotFoundException("TestEnvironment", "id", envId));
        env.setIsActive(false);
        environmentRepository.save(env);
    }

    // ==================== Additional Traceability Methods ====================

    @Transactional(readOnly = true)
    public List<RequirementLinkResponse> getRequirementLinks(UUID testId) {
        return requirementLinkRepository.findByTestIssueId(testId).stream()
                .map(this::mapToRequirementLinkResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeRequirementLink(UUID linkId) {
        requirementLinkRepository.deleteById(linkId);
    }

    @Transactional(readOnly = true)
    public List<DefectLinkResponse> getDefectLinks(UUID executionId) {
        return defectLinkRepository.findByTestExecutionId(executionId).stream()
                .map(this::mapToDefectLinkResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TraceabilityMatrixResponse.RequirementRow getRequirementCoverage(String requirementKey) {
        List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(requirementKey);

        List<TraceabilityMatrixResponse.TestCoverage> coverages = links.stream()
                .map(link -> TraceabilityMatrixResponse.TestCoverage.builder()
                        .testId(link.getTestIssueId())
                        .status(link.getLastExecutionStatus())
                        .coverageStatus(link.getCoverageStatus())
                        .build())
                .collect(Collectors.toList());

        return TraceabilityMatrixResponse.RequirementRow.builder()
                .requirementKey(requirementKey)
                .testCount(coverages.size())
                .coverageStatus(calculateCoverageStatus(coverages))
                .tests(coverages)
                .build();
    }

    // ==================== Test Environment Methods ====================

    @Transactional
    public TestEnvironmentResponse createTestEnvironment(UUID projectId, CreateEnvironmentRequest request, UUID userId) {
        TestEnvironment env = TestEnvironment.builder()
                .projectId(projectId)
                .name(request.getName())
                .description(request.getDescription())
                .environmentType(request.getEnvironmentType())
                .config(request.getConfig())
                .url(request.getUrl())
                .variables(request.getVariables())
                .credentials(request.getCredentials())
                .isActive(true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdBy(userId)
                .build();

        env = environmentRepository.save(env);
        return mapToEnvironmentResponse(env);
    }

    @Transactional(readOnly = true)
    public List<TestEnvironmentResponse> getEnvironmentsByProject(UUID projectId) {
        return environmentRepository.findByProjectIdAndIsActiveTrueOrderBySortOrderAsc(projectId).stream()
                .map(this::mapToEnvironmentResponse)
                .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private String buildTestStepsJson(List<CreateTestRequest.TestStepDto> steps) {
        if (steps == null || steps.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            log.error("Error serializing test steps", e);
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<CreateTestRequest.TestStepDto> parseTestStepsJson(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CreateTestRequest.TestStepDto.class));
        } catch (JsonProcessingException e) {
            log.error("Error parsing test steps", e);
            return new ArrayList<>();
        }
    }

    private String buildFolderPath(UUID parentId, String name) {
        if (parentId == null) return "/" + name;
        TestRepositoryFolder parent = folderRepository.findById(parentId).orElse(null);
        if (parent == null) return "/" + name;
        return parent.getPath() + "/" + name;
    }

    private int calculateFolderDepth(UUID parentId) {
        if (parentId == null) return 0;
        TestRepositoryFolder parent = folderRepository.findById(parentId).orElse(null);
        return parent != null ? parent.getDepth() + 1 : 0;
    }

    private int countTestsForExecution(CreateExecutionRequest request) {
        if (request.getTestId() != null) return 1;
        if (request.getTestSetId() != null) {
            return (int) issueRepository.findByProjectIdAndIssueTypeName(request.getProjectId(), "Test").stream()
                    .filter(t -> request.getTestSetId().equals(t.getTestSetId()))
                    .count();
        }
        if (request.getTestPlanId() != null) {
            List<TestPlanItem> items = testPlanItemRepository.findByTestPlanIdOrderByExecutionOrderAsc(request.getTestPlanId());
            int count = 0;
            for (TestPlanItem item : items) {
                count += (int) issueRepository.findByProjectIdAndIssueTypeName(request.getProjectId(), "Test").stream()
                        .filter(t -> item.getTestSetId().equals(t.getTestSetId()))
                        .count();
            }
            return count;
        }
        return 0;
    }

    private void updateExecutionSummary(UUID executionId) {
        TestExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) return;

        List<StepResult> results = stepResultRepository.findByExecutionIdOrderByStepOrderAsc(executionId);

        long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        long blocked = results.stream().filter(r -> "BLOCKED".equals(r.getStatus())).count();
        long skipped = results.stream().filter(r -> "SKIPPED".equals(r.getStatus())).count();

        execution.setPassedTests((int) passed);
        execution.setFailedTests((int) failed);
        execution.setBlockedTests((int) blocked);
        execution.setSkippedTests((int) skipped);
        execution.setNotRunTests(execution.getTotalTests() - (int) (passed + failed + blocked + skipped));

        executionRepository.save(execution);
    }

    private String calculateExecutionStatus(UUID executionId) {
        TestExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) return "UNKNOWN";

        if (execution.getNotRunTests() > 0) return "RUNNING";
        if (execution.getBlockedTests() > 0) return "BLOCKED";
        if (execution.getFailedTests() > 0) return "FAILED";
        return "PASSED";
    }

    private void recordExecutionHistory(TestExecution execution) {
        List<Issue> tests = new ArrayList<>();
        if (execution.getTestId() != null) {
            Issue test = issueRepository.findById(execution.getTestId()).orElse(null);
            if (test != null) tests.add(test);
        } else if (execution.getTestSetId() != null) {
            tests = issueRepository.findByProjectIdAndIssueTypeName(execution.getProjectId(), "Test").stream()
                    .filter(t -> execution.getTestSetId().equals(t.getTestSetId()))
                    .collect(Collectors.toList());
        }

        for (Issue test : tests) {
            List<StepResult> results = stepResultRepository.findByExecutionIdAndIssueIdAndStepOrder(execution.getId(), test.getId(), 1)
                    .map(List::of)
                    .orElse(stepResultRepository.findByIssueIdOrderByStepOrderAsc(test.getId()));

            String status = "NOT_RUN";
            if (!results.isEmpty()) {
                long failed = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
                long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
                if (failed > 0) status = "FAILED";
                else if (passed > 0) status = "PASSED";
            }

            TestExecutionHistory history = TestExecutionHistory.builder()
                    .testIssueId(test.getId())
                    .executionId(execution.getId())
                    .status(status)
                    .executedBy(execution.getTesterId())
                    .testEnv(execution.getTestEnv())
                    .durationMs(execution.getDurationSeconds() != null ? execution.getDurationSeconds() * 1000 : null)
                    .build();
            historyRepository.save(history);
        }
    }

    private String calculateCoverageStatus(List<TraceabilityMatrixResponse.TestCoverage> coverages) {
        if (coverages.isEmpty()) return "NOT_COVERED";
        long passed = coverages.stream().filter(c -> "PASSED".equals(c.getStatus())).count();
        long total = coverages.size();
        double rate = (double) passed / total;
        if (rate == 1.0) return "COVERED";
        if (rate > 0) return "PARTIAL";
        return "NOT_COVERED";
    }

    private double calculateOverallCoverage(List<TraceabilityMatrixResponse.RequirementRow> rows) {
        if (rows.isEmpty()) return 0.0;
        long covered = rows.stream().filter(r -> !"NOT_COVERED".equals(r.getCoverageStatus())).count();
        return (double) covered / rows.size() * 100;
    }

    // ==================== Mapping Methods ====================

    private TestResponse mapToTestResponse(Issue test) {
        List<CreateTestRequest.TestStepDto> steps = parseTestStepsJson(test.getTestSteps());
        List<TestResponse.StepResponse> stepResponses = steps.stream()
                .map(s -> TestResponse.StepResponse.builder()
                        .stepOrder(s.getStepOrder())
                        .stepType(s.getStepType())
                        .description(s.getDescription())
                        .testData(s.getTestData())
                        .expectedResult(s.getExpectedResult())
                        .build())
                .collect(Collectors.toList());

        // Get last execution status
        String lastStatus = null;
        if (test.getTestExecutionId() != null) {
            TestExecution lastExec = executionRepository.findById(test.getTestExecutionId()).orElse(null);
            if (lastExec != null) lastStatus = lastExec.getStatus();
        }

        return TestResponse.builder()
                .id(test.getId())
                .projectId(test.getProjectId())
                .issueKey(test.getIssueKey())
                .name(test.getTitle())
                .description(test.getDescription())
                .testType(test.getTestType())
                .testStatus(test.getTestStatus())
                .priority(test.getTestPriority())
                .ownerId(test.getTestOwnerId())
                .labels(test.getLabels() != null ? Arrays.asList(test.getLabels()) : List.of())
                .requirementKeys(test.getRequirementKeys() != null ? Arrays.asList(test.getRequirementKeys()) : List.of())
                .gherkinFeatureKey(test.getGherkinFeatureKey())
                .gherkinScenarioId(test.getGherkinScenarioId())
                .testSetId(test.getTestSetId())
                .folderId(test.getTestRepositoryFolderId())
                .lastExecutionStatus(lastStatus)
                .steps(stepResponses)
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }

    private TestRepositoryFolderResponse mapToFolderResponse(TestRepositoryFolder folder) {
        return TestRepositoryFolderResponse.builder()
                .id(folder.getId())
                .projectId(folder.getProjectId())
                .parentFolderId(folder.getParentFolderId())
                .name(folder.getName())
                .description(folder.getDescription())
                .path(folder.getPath())
                .depth(folder.getDepth())
                .sortOrder(folder.getSortOrder())
                .isSmartFolder(folder.getIsSmartFolder())
                .smartFolderQuery(folder.getSmartFolderQuery())
                .createdAt(folder.getCreatedAt())
                .build();
    }

    private TestSetResponse mapToTestSetResponse(TestSet testSet) {
        return TestSetResponse.builder()
                .id(testSet.getId())
                .projectId(testSet.getProjectId())
                .folderId(testSet.getFolderId())
                .name(testSet.getName())
                .description(testSet.getDescription())
                .testType(testSet.getTestType())
                .labels(testSet.getLabels() != null ? Arrays.asList(testSet.getLabels()) : List.of())
                .testCount(testSet.getTestCount())
                .requirementKeys(testSet.getRequirementKeys() != null ? Arrays.asList(testSet.getRequirementKeys()) : List.of())
                .status(testSet.getStatus())
                .ownerId(testSet.getOwnerId())
                .archived(testSet.getArchived())
                .createdAt(testSet.getCreatedAt())
                .updatedAt(testSet.getUpdatedAt())
                .build();
    }

    private TestPlanResponse mapToTestPlanResponse(TestPlan plan) {
        List<TestPlanItem> items = testPlanItemRepository.findByTestPlanIdOrderByExecutionOrderAsc(plan.getId());
        List<UUID> testSetIds = items.stream().map(TestPlanItem::getTestSetId).collect(Collectors.toList());

        return TestPlanResponse.builder()
                .id(plan.getId())
                .projectId(plan.getProjectId())
                .name(plan.getName())
                .description(plan.getDescription())
                .testType(plan.getTestType())
                .labels(plan.getLabels() != null ? Arrays.asList(plan.getLabels()) : List.of())
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .targetVersion(plan.getTargetVersion())
                .environment(plan.getEnvironment())
                .ownerId(plan.getOwnerId())
                .testSetIds(testSetIds)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private TestExecutionResponse mapToExecutionResponse(TestExecution execution) {
        return TestExecutionResponse.builder()
                .id(execution.getId())
                .projectId(execution.getProjectId())
                .testPlanId(execution.getTestPlanId())
                .testSetId(execution.getTestSetId())
                .testId(execution.getTestId())
                .name(execution.getName())
                .description(execution.getDescription())
                .status(execution.getStatus())
                .testEnv(execution.getTestEnv())
                .testerId(execution.getTesterId())
                .testCycle(execution.getTestCycle())
                .totalTests(execution.getTotalTests())
                .passedTests(execution.getPassedTests())
                .failedTests(execution.getFailedTests())
                .blockedTests(execution.getBlockedTests())
                .skippedTests(execution.getSkippedTests())
                .notRunTests(execution.getNotRunTests())
                .passRate(execution.getPassRate())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .durationSeconds(execution.getDurationSeconds())
                .ciBuildUrl(execution.getCiBuildUrl())
                .ciJobName(execution.getCiJobName())
                .ciBuildNumber(execution.getCiBuildNumber())
                .branch(execution.getBranch())
                .commitSha(execution.getCommitSha())
                .createdAt(execution.getCreatedAt())
                .build();
    }

    private StepResultResponse mapToStepResultResponse(StepResult result) {
        return StepResultResponse.builder()
                .id(result.getId())
                .executionId(result.getExecutionId())
                .issueId(result.getIssueId())
                .stepOrder(result.getStepOrder())
                .stepType(result.getStepType())
                .stepDescription(result.getStepDescription())
                .expectedResult(result.getExpectedResult())
                .status(result.getStatus())
                .actualResult(result.getActualResult())
                .defectKey(result.getDefectKey())
                .defectSeverity(result.getDefectSeverity())
                .executedAt(result.getExecutedAt())
                .executionTimeMs(result.getExecutionTimeMs())
                .comment(result.getComment())
                .createdAt(result.getCreatedAt())
                .build();
    }

    private RequirementLinkResponse mapToRequirementLinkResponse(RequirementLink link) {
        return RequirementLinkResponse.builder()
                .id(link.getId())
                .requirementKey(link.getRequirementKey())
                .requirementSummary(link.getRequirementSummary())
                .requirementType(link.getRequirementType())
                .testIssueId(link.getTestIssueId())
                .coverageStatus(link.getCoverageStatus())
                .lastTestExecutionId(link.getLastTestExecutionId())
                .lastExecutionStatus(link.getLastExecutionStatus())
                .createdAt(link.getCreatedAt())
                .build();
    }

    private DefectLinkResponse mapToDefectLinkResponse(DefectLink link) {
        return DefectLinkResponse.builder()
                .id(link.getId())
                .defectKey(link.getDefectKey())
                .defectSummary(link.getDefectSummary())
                .defectType(link.getDefectType())
                .testExecutionId(link.getTestExecutionId())
                .stepResultId(link.getStepResultId())
                .testIssueId(link.getTestIssueId())
                .severity(link.getSeverity())
                .status(link.getStatus())
                .priority(link.getPriority())
                .linkedAt(link.getLinkedAt())
                .build();
    }

    private TestEnvironmentResponse mapToEnvironmentResponse(TestEnvironment env) {
        return TestEnvironmentResponse.builder()
                .id(env.getId())
                .projectId(env.getProjectId())
                .name(env.getName())
                .description(env.getDescription())
                .environmentType(env.getEnvironmentType())
                .config(env.getConfig())
                .url(env.getUrl())
                .variables(env.getVariables())
                .isActive(env.getIsActive())
                .sortOrder(env.getSortOrder())
                .createdAt(env.getCreatedAt())
                .build();
    }
}