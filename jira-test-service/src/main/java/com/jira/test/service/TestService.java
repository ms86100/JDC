package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

    private final TestIssueRepository testIssueRepository;
    private final TestStepRepository testStepRepository;
    private final TestSetRepository testSetRepository;
    private final TestSetItemRepository testSetItemRepository;
    private final TestPlanRepository testPlanRepository;
    private final TestPlanItemRepository testPlanItemRepository;
    private final RequirementLinkRepository requirementLinkRepository;

    @Transactional
    public TestResponse createTest(CreateTestRequest request) {
        log.info("Creating test: {} for project: {}", request.getName(), request.getProjectId());

        if (testIssueRepository.existsByProjectIdAndName(request.getProjectId(), request.getName())) {
            throw new DuplicateResourceException("Test with name '" + request.getName() + "' already exists in this project");
        }

        TestIssue test = TestIssue.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .testType(request.getTestType() != null ? request.getTestType() : "MANUAL")
                .labels(request.getLabels() != null ? request.getLabels() : List.of())
                .priority(request.getPriority())
                .ownerId(request.getOwnerId())
                .requirementKeys(request.getRequirementKeys())
                .build();

        test = testIssueRepository.save(test);

        if (request.getSteps() != null && !request.getSteps().isEmpty()) {
            int order = 1;
            for (CreateTestRequest.TestStepDto stepDto : request.getSteps()) {
                TestStep step = TestStep.builder()
                        .testId(test.getId())
                        .stepOrder(stepDto.getStepOrder() != null ? stepDto.getStepOrder() : order++)
                        .stepType(stepDto.getStepType() != null ? stepDto.getStepType() : "WHEN")
                        .description(stepDto.getDescription())
                        .testData(stepDto.getTestData())
                        .expectedResult(stepDto.getExpectedResult())
                        .build();
                testStepRepository.save(step);
            }
        }

        log.info("Test created with id: {}", test.getId());
        return mapToTestResponse(test);
    }

    @Transactional(readOnly = true)
    public TestResponse getTest(UUID testId) {
        TestIssue test = testIssueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
        return mapToTestResponse(test);
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getTestsByProject(UUID projectId) {
        return testIssueRepository.findByProjectIdAndArchivedFalse(projectId).stream()
                .map(this::mapToTestResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getTestsBySet(UUID testSetId) {
        return testIssueRepository.findByTestSetId(testSetId).stream()
                .map(this::mapToTestResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getTestsByRequirement(String requirementKey) {
        return testIssueRepository.findByRequirementKey(requirementKey).stream()
                .map(this::mapToTestResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestResponse updateTest(UUID testId, CreateTestRequest request) {
        TestIssue test = testIssueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        if (request.getName() != null) test.setName(request.getName());
        if (request.getDescription() != null) test.setDescription(request.getDescription());
        if (request.getTestType() != null) test.setTestType(request.getTestType());
        if (request.getLabels() != null) test.setLabels(request.getLabels());
        if (request.getPriority() != null) test.setPriority(request.getPriority());
        if (request.getOwnerId() != null) test.setOwnerId(request.getOwnerId());

        test = testIssueRepository.save(test);
        return mapToTestResponse(test);
    }

    @Transactional
    public void deleteTest(UUID testId) {
        TestIssue test = testIssueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
        test.setArchived(true);
        testIssueRepository.save(test);
        log.info("Test archived: {}", testId);
    }

    // ==================== Test Set ====================

    @Transactional
    public TestSetResponse createTestSet(CreateTestSetRequest request) {
        log.info("Creating test set: {} for project: {}", request.getName(), request.getProjectId());

        TestSet set = TestSet.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .testType(request.getTestType() != null ? request.getTestType() : "MANUAL")
                .labels(request.getLabels() != null ? request.getLabels() : List.of())
                .build();

        set = testSetRepository.save(set);
        log.info("Test set created with id: {}", set.getId());
        return mapToTestSetResponse(set);
    }

    @Transactional(readOnly = true)
    public TestSetResponse getTestSet(UUID setId) {
        TestSet set = testSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", setId));
        return mapToTestSetResponse(set);
    }

    @Transactional(readOnly = true)
    public List<TestSetResponse> getTestSetsByProject(UUID projectId) {
        return testSetRepository.findByProjectIdAndArchivedFalse(projectId).stream()
                .map(this::mapToTestSetResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestSetResponse addTestToSet(UUID setId, UUID testId) {
        TestSet set = testSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", setId));
        TestIssue test = testIssueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        if (testSetItemRepository.findByTestSetIdAndTestId(setId, testId).isPresent()) {
            throw new DuplicateResourceException("Test already in set");
        }

        TestSetItem item = TestSetItem.builder().testSetId(setId).testId(testId).build();
        testSetItemRepository.save(item);

        set.setTestCount((int) testSetItemRepository.countByTestSetId(setId));
        testSetRepository.save(set);

        test.setTestSetId(setId);
        testIssueRepository.save(test);

        return mapToTestSetResponse(set);
    }

    @Transactional
    public void removeTestFromSet(UUID setId, UUID testId) {
        testSetItemRepository.findByTestSetIdAndTestId(setId, testId)
                .ifPresent(item -> testSetItemRepository.delete(item));

        TestSet set = testSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", setId));
        set.setTestCount((int) testSetItemRepository.countByTestSetId(setId));
        testSetRepository.save(set);
    }

    // ==================== Test Plan ====================

    @Transactional
    public TestPlanResponse createTestPlan(CreateTestPlanRequest request) {
        log.info("Creating test plan: {} for project: {}", request.getName(), request.getProjectId());

        TestPlan plan = TestPlan.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .labels(request.getLabels() != null ? request.getLabels() : List.of())
                .createdBy(request.getCreatedBy())
                .build();

        plan = testPlanRepository.save(plan);
        log.info("Test plan created with id: {}", plan.getId());
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
        return testPlanRepository.findByProjectId(projectId).stream()
                .map(this::mapToTestPlanResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestPlanResponse addTestSetToPlan(UUID planId, UUID setId) {
        TestPlan plan = testPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("TestPlan", "id", planId));
        TestSet set = testSetRepository.findById(setId)
                .orElseThrow(() -> new ResourceNotFoundException("TestSet", "id", setId));

        TestPlanItem item = TestPlanItem.builder()
                .testPlanId(planId)
                .testSetId(setId)
                .executionOrder(testPlanItemRepository.findByTestPlanIdOrderByExecutionOrderAsc(planId).size() + 1)
                .build();
        testPlanItemRepository.save(item);

        return mapToTestPlanResponse(plan);
    }

    // ==================== Mapping ====================

    private TestResponse mapToTestResponse(TestIssue test) {
        List<TestStep> testSteps = testStepRepository.findByTestIdOrderByStepOrderAsc(test.getId());
        List<TestResponse.StepResponse> steps = testSteps.stream().map(s -> TestResponse.StepResponse.builder()
                        .id(s.getId())
                        .stepOrder(s.getStepOrder())
                        .stepType(s.getStepType())
                        .description(s.getDescription())
                        .testData(s.getTestData())
                        .expectedResult(s.getExpectedResult())
                        .createdAt(s.getCreatedAt())
                        .build()).collect(Collectors.toList());

        return TestResponse.builder()
                .id(test.getId())
                .projectId(test.getProjectId())
                .name(test.getName())
                .description(test.getDescription())
                .testType(test.getTestType())
                .status(test.getStatus())
                .labels(test.getLabels())
                .priority(test.getPriority())
                .ownerId(test.getOwnerId())
                .requirementKeys(test.getRequirementKeys())
                .gherkinFeatureKey(test.getGherkinFeatureKey())
                .gherkinScenarioId(test.getGherkinScenarioId())
                .testSetId(test.getTestSetId())
                .archived(test.getArchived())
                .steps(steps)
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }

    private TestSetResponse mapToTestSetResponse(TestSet set) {
        return TestSetResponse.builder()
                .id(set.getId())
                .projectId(set.getProjectId())
                .name(set.getName())
                .description(set.getDescription())
                .testType(set.getTestType())
                .labels(set.getLabels())
                .testCount(set.getTestCount())
                .archived(set.getArchived())
                .createdAt(set.getCreatedAt())
                .updatedAt(set.getUpdatedAt())
                .build();
    }

    private TestPlanResponse mapToTestPlanResponse(TestPlan plan) {
        return TestPlanResponse.builder()
                .id(plan.getId())
                .projectId(plan.getProjectId())
                .name(plan.getName())
                .description(plan.getDescription())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .status(plan.getStatus())
                .labels(plan.getLabels())
                .createdBy(plan.getCreatedBy())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}