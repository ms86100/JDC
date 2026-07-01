package com.jira.issue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.issue.dto.*;
import com.jira.issue.service.TestManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Test Controllers
 * Phase 19 - Testing Strategy
 */
@WebMvcTest(TestController.class)
@AutoConfigureGraphQlTester
class TestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestManagementService testManagementService;

    private UUID projectId;
    private UUID testId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        testId = UUID.randomUUID();
    }

    @Test
    void createTest_ReturnsCreated() throws Exception {
        CreateTestRequest request = CreateTestRequest.builder()
                .name("Login Test")
                .description("Test login functionality")
                .testType("MANUAL")
                .build();

        TestResponse response = TestResponse.builder()
                .id(testId)
                .projectId(projectId)
                .issueKey("TEST-1")
                .name("Login Test")
                .testType("MANUAL")
                .testStatus("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();

        when(testManagementService.createTest(eq(projectId), any(CreateTestRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/tests")
                        .param("projectId", projectId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Login Test"))
                .andExpect(jsonPath("$.testType").value("MANUAL"));
    }

    @Test
    void getTest_ReturnsTest() throws Exception {
        TestResponse response = TestResponse.builder()
                .id(testId)
                .projectId(projectId)
                .issueKey("TEST-1")
                .name("Login Test")
                .testType("MANUAL")
                .build();

        when(testManagementService.getTestById(testId)).thenReturn(response);

        mockMvc.perform(get("/api/tests/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueKey").value("TEST-1"));
    }

    @Test
    void searchTests_ReturnsPagedResults() throws Exception {
        TestResponse test1 = TestResponse.builder()
                .id(testId)
                .issueKey("TEST-1")
                .name("Test 1")
                .build();

        TestConnectionResponse connection = TestConnectionResponse.builder()
                .edges(List.of(TestEdge.builder().node(test1).cursor("abc").build()))
                .totalCount(1)
                .build();

        when(testManagementService.searchTests(any(), any())).thenReturn(connection);

        mockMvc.perform(get("/api/tests/search")
                        .param("projectId", projectId.toString())
                        .param("testType", "MANUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void deleteTest_ReturnsNoContent() throws Exception {
        when(testManagementService.deleteTest(testId)).thenReturn(true);

        mockMvc.perform(delete("/api/tests/{id}", testId))
                .andExpect(status().isNoContent());
    }

    @Test
    void createTestSet_ReturnsCreated() throws Exception {
        CreateTestSetRequest request = CreateTestSetRequest.builder()
                .projectId(projectId)
                .name("Regression Suite")
                .build();

        TestSetResponse response = TestSetResponse.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .name("Regression Suite")
                .testCount(0)
                .build();

        when(testManagementService.createTestSet(any())).thenReturn(response);

        mockMvc.perform(post("/api/test-sets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Regression Suite"));
    }

    @Test
    void createTestPlan_ReturnsCreated() throws Exception {
        CreateTestPlanRequest request = CreateTestPlanRequest.builder()
                .projectId(projectId)
                .name("Sprint 1 Plan")
                .testCycle("SPRINT-1")
                .build();

        TestPlanResponse response = TestPlanResponse.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .name("Sprint 1 Plan")
                .testCount(0)
                .build();

        when(testManagementService.createTestPlan(any())).thenReturn(response);

        mockMvc.perform(post("/api/test-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sprint 1 Plan"));
    }

    @Test
    void startExecution_ReturnsExecution() throws Exception {
        CreateExecutionRequest request = CreateExecutionRequest.builder()
                .testIds(List.of(testId))
                .name("Run 1")
                .testEnv("DEFAULT")
                .build();

        TestExecutionResponse response = TestExecutionResponse.builder()
                .id(UUID.randomUUID())
                .testId(testId)
                .status("RUNNING")
                .name("Run 1")
                .build();

        when(testManagementService.startExecution(any())).thenReturn(response);

        mockMvc.perform(post("/api/test-executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void linkRequirement_ReturnsLinks() throws Exception {
        String requirementKey = "PROJ-123";
        List<RequirementLinkResponse> response = List.of(
                RequirementLinkResponse.builder()
                        .id(UUID.randomUUID())
                        .requirementKey(requirementKey)
                        .testId(testId)
                        .build()
        );

        when(testManagementService.linkRequirement(eq(requirementKey), any())).thenReturn(response);

        mockMvc.perform(post("/api/requirements/links")
                        .param("requirementKey", requirementKey)
                        .param("testIds", testId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getTestSummary_ReturnsSummary() throws Exception {
        TestSummaryResponse response = TestSummaryResponse.builder()
                .totalTests(100)
                .manualTests(50)
                .automatedTests(30)
                .bddTests(20)
                .totalTestSets(5)
                .totalTestPlans(3)
                .totalExecutions(200)
                .passRate(85.0)
                .build();

        when(testManagementService.getTestSummary(projectId)).thenReturn(response);

        mockMvc.perform(get("/api/reports/summary")
                        .param("projectId", projectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTests").value(100))
                .andExpect(jsonPath("$.passRate").value(85.0));
    }
}