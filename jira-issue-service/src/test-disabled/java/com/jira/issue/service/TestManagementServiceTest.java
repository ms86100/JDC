package com.jira.issue.service;

import com.jira.issue.dto.*;
import com.jira.issue.entity.*;
import com.jira.issue.exception.ValidationException;
import com.jira.issue.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TestManagementService
 * Phase 19 - Testing Strategy
 */
@ExtendWith(MockitoExtension.class)
class TestManagementServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueTypeRepository issueTypeRepository;

    @Mock
    private IssueStatusRepository issueStatusRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TestRepositoryFolderRepository folderRepository;

    @Mock
    private TestSetRepository testSetRepository;

    @Mock
    private TestPlanRepository testPlanRepository;

    @Mock
    private TestExecutionRepository executionRepository;

    @Mock
    private StepResultRepository stepResultRepository;

    @Mock
    private RequirementLinkRepository requirementLinkRepository;

    @Mock
    private DefectLinkRepository defectLinkRepository;

    @Mock
    private TestEnvironmentRepository environmentRepository;

    @Mock
    private TestDatasetRepository datasetRepository;

    @Mock
    private TestExecutionHistoryRepository historyRepository;

    @Mock
    private TestEvidenceRepository evidenceRepository;

    @Mock
    private SharedStepRepository sharedStepRepository;

    @Mock
    private SharedStepUsageRepository sharedStepUsageRepository;

    @Mock
    private TestVersionRepository versionRepository;

    @InjectMocks
    private TestManagementService testManagementService;

    private UUID projectId;
    private UUID testId;
    private Project mockProject;
    private IssueType mockIssueType;
    private IssueStatus mockStatus;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        testId = UUID.randomUUID();

        mockProject = Project.builder()
                .id(projectId)
                .projectKey("TEST")
                .name("Test Project")
                .build();

        mockIssueType = IssueType.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .description("Test issue type")
                .icon("test-icon")
                .build();

        mockStatus = IssueStatus.builder()
                .id(UUID.randomUUID())
                .name("To Do")
                .statusCategory("TODO")
                .build();
    }

    @Test
    void createTest_Success() {
        CreateTestRequest request = CreateTestRequest.builder()
                .name("Login Test")
                .description("Test user login functionality")
                .testType("MANUAL")
                .testStatus("DRAFT")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(issueTypeRepository.findByName("Test")).thenReturn(Optional.of(mockIssueType));
        when(issueStatusRepository.findByName("To Do")).thenReturn(Optional.of(mockStatus));
        when(issueRepository.count()).thenReturn(1L);
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> {
            Issue issue = invocation.getArgument(0);
            issue.setId(testId);
            return issue;
        });

        TestResponse response = testManagementService.createTest(projectId, request, UUID.randomUUID());

        assertNotNull(response);
        assertEquals("Login Test", response.getName());
        assertEquals("MANUAL", response.getTestType());
        assertEquals("DRAFT", response.getTestStatus());
        verify(issueRepository, times(1)).save(any(Issue.class));
    }

    @Test
    void createTest_ProjectNotFound() {
        CreateTestRequest request = CreateTestRequest.builder()
                .name("Test")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
                testManagementService.createTest(projectId, request, UUID.randomUUID()));
    }

    @Test
    void getTestById_Success() {
        Issue mockIssue = createMockIssue();

        when(issueRepository.findById(testId)).thenReturn(Optional.of(mockIssue));

        TestResponse response = testManagementService.getTestById(testId);

        assertNotNull(response);
        assertEquals(testId, response.getId());
        assertEquals("TEST-1", response.getIssueKey());
    }

    @Test
    void getTestById_NotFound() {
        when(issueRepository.findById(testId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
                testManagementService.getTestById(testId));
    }

    @Test
    void searchTests_WithFilters() {
        Issue mockIssue = createMockIssue();
        List<Issue> issues = List.of(mockIssue);

        when(issueRepository.searchTests(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(issues);
        when(issueRepository.countByFilters(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        TestFilterInput filter = TestFilterInput.builder()
                .projectId(projectId)
                .testType("MANUAL")
                .build();

        PaginationInput pagination = PaginationInput.builder()
                .page(0)
                .size(20)
                .build();

        TestConnectionResponse response = testManagementService.searchTests(filter, pagination);

        assertNotNull(response);
        assertEquals(1, response.getTotalCount());
    }

    @Test
    void createTestSet_Success() {
        CreateTestSetRequest request = CreateTestSetRequest.builder()
                .projectId(projectId)
                .name("Regression Tests")
                .description("All regression tests")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(testSetRepository.save(any(TestSet.class))).thenAnswer(invocation -> {
            TestSet set = invocation.getArgument(0);
            set.setId(UUID.randomUUID());
            return set;
        });

        TestSetResponse response = testManagementService.createTestSet(request);

        assertNotNull(response);
        assertEquals("Regression Tests", response.getName());
        verify(testSetRepository, times(1)).save(any(TestSet.class));
    }

    @Test
    void createTestPlan_Success() {
        CreateTestPlanRequest request = CreateTestPlanRequest.builder()
                .projectId(projectId)
                .name("Sprint 1 Tests")
                .testCycle("SPRINT-1")
                .testEnvironment("STAGING")
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(testPlanRepository.save(any(TestPlan.class))).thenAnswer(invocation -> {
            TestPlan plan = invocation.getArgument(0);
            plan.setId(UUID.randomUUID());
            return plan;
        });

        TestPlanResponse response = testManagementService.createTestPlan(request);

        assertNotNull(response);
        assertEquals("Sprint 1 Tests", response.getName());
        assertEquals("SPRINT-1", response.getTestCycle());
    }

    @Test
    void startExecution_Success() {
        Issue mockIssue = createMockIssue();
        CreateExecutionRequest request = CreateExecutionRequest.builder()
                .testIds(List.of(testId))
                .name("Run 1")
                .testEnv("DEFAULT")
                .build();

        when(issueRepository.findById(testId)).thenReturn(Optional.of(mockIssue));
        when(executionRepository.save(any(TestExecution.class))).thenAnswer(invocation -> {
            TestExecution exec = invocation.getArgument(0);
            exec.setId(UUID.randomUUID());
            return exec;
        });

        TestExecutionResponse response = testManagementService.startExecution(request);

        assertNotNull(response);
        assertEquals("RUNNING", response.getStatus());
        verify(executionRepository, times(1)).save(any(TestExecution.class));
    }

    @Test
    void linkRequirement_Success() {
        String requirementKey = "PROJ-123";
        List<UUID> testIds = List.of(testId);
        Issue mockIssue = createMockIssue();

        when(issueRepository.findById(testId)).thenReturn(Optional.of(mockIssue));
        when(requirementLinkRepository.existsByRequirementKeyAndTestId(requirementKey, testId))
                .thenReturn(false);
        when(requirementLinkRepository.save(any(RequirementLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<RequirementLinkResponse> response = testManagementService.linkRequirement(requirementKey, testIds);

        assertNotNull(response);
        assertEquals(1, response.size());
        verify(requirementLinkRepository, times(1)).save(any(RequirementLink.class));
    }

    @Test
    void getTestSummary_Success() {
        when(issueRepository.countByProjectIdAndIssueType(projectId, "Test")).thenReturn(10L);
        when(issueRepository.countByProjectIdAndTestType(projectId, "MANUAL")).thenReturn(5L);
        when(issueRepository.countByProjectIdAndTestType(projectId, "AUTOMATED")).thenReturn(3L);
        when(issueRepository.countByProjectIdAndTestType(projectId, "BDD")).thenReturn(2L);
        when(issueRepository.countByProjectIdAndTestStatus(projectId, "DRAFT")).thenReturn(2L);
        when(issueRepository.countByProjectIdAndTestStatus(projectId, "READY")).thenReturn(5L);
        when(issueRepository.countByProjectIdAndTestStatus(projectId, "APPROVED")).thenReturn(3L);
        when(testSetRepository.countByProjectId(projectId)).thenReturn(3L);
        when(testPlanRepository.countByProjectId(projectId)).thenReturn(2L);
        when(executionRepository.countByProjectId(projectId)).thenReturn(50L);
        when(executionRepository.getPassRateByProjectId(projectId)).thenReturn(85.5);

        TestSummaryResponse response = testManagementService.getTestSummary(projectId);

        assertNotNull(response);
        assertEquals(10, response.getTotalTests());
        assertEquals(5, response.getManualTests());
        assertEquals(3, response.getAutomatedTests());
        assertEquals(2, response.getBddTests());
        assertEquals(3, response.getTotalTestSets());
        assertEquals(2, response.getTotalTestPlans());
        assertEquals(50, response.getTotalExecutions());
    }

    private Issue createMockIssue() {
        return Issue.builder()
                .id(testId)
                .projectId(projectId)
                .issueKey("TEST-1")
                .title("Test Issue")
                .issueType(mockIssueType)
                .status(mockStatus)
                .testType("MANUAL")
                .testStatus("DRAFT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}