package com.jira.issue.service;

import com.jira.issue.config.CacheConfig;
import com.jira.issue.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Cached Test Management Service
 * Phase 13 - Redis Caching Layer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachedTestManagementService {

    private final TestManagementService testManagementService;

    @Cacheable(value = CacheConfig.TESTS_CACHE, key = "#testId")
    public TestResponse getTestById(UUID testId) {
        log.debug("Cache MISS - Fetching test: {}", testId);
        return testManagementService.getTestById(testId);
    }

    @CacheEvict(value = CacheConfig.TESTS_CACHE, key = "#testId")
    public void evictTest(UUID testId) {
        log.debug("Cache EVICT - Test: {}", testId);
    }

    @Cacheable(value = CacheConfig.TESTS_CACHE, key = "'project:' + #filter.projectId + ':search:' + #filter.search + ':type:' + #filter.testType + ':status:' + #filter.testStatus")
    public TestConnectionResponse searchTests(TestFilterInput filter, PaginationInput pagination) {
        log.debug("Cache MISS - Searching tests for project: {}", filter != null ? filter.getProjectId() : null);
        return testManagementService.searchTests(filter, pagination);
    }

    @Cacheable(value = CacheConfig.TEST_SETS_CACHE, key = "#testSetId")
    public TestSetResponse getTestSetById(UUID testSetId) {
        log.debug("Cache MISS - Fetching test set: {}", testSetId);
        return testManagementService.getTestSetById(testSetId);
    }

    @Cacheable(value = CacheConfig.TEST_SETS_CACHE, key = "'project:' + #projectId")
    public List<TestSetResponse> getTestSetsByProject(UUID projectId) {
        log.debug("Cache MISS - Fetching test sets for project: {}", projectId);
        return testManagementService.getTestSetsByProject(projectId);
    }

    @Cacheable(value = CacheConfig.TEST_PLANS_CACHE, key = "#testPlanId")
    public TestPlanResponse getTestPlanById(UUID testPlanId) {
        log.debug("Cache MISS - Fetching test plan: {}", testPlanId);
        return testManagementService.getTestPlanById(testPlanId);
    }

    @Cacheable(value = CacheConfig.TEST_PLANS_CACHE, key = "'project:' + #projectId")
    public List<TestPlanResponse> getTestPlansByProject(UUID projectId) {
        log.debug("Cache MISS - Fetching test plans for project: {}", projectId);
        return testManagementService.getTestPlansByProject(projectId);
    }

    @Cacheable(value = CacheConfig.TEST_EXECUTIONS_CACHE, key = "#executionId")
    public TestExecutionResponse getExecutionById(UUID executionId) {
        log.debug("Cache MISS - Fetching execution: {}", executionId);
        return testManagementService.getExecutionById(executionId);
    }

    @Cacheable(value = CacheConfig.TEST_EXECUTIONS_CACHE, key = "'test:' + #testId")
    public List<TestExecutionResponse> getExecutionsByTest(UUID testId) {
        log.debug("Cache MISS - Fetching executions for test: {}", testId);
        return testManagementService.getExecutionsByTest(testId);
    }

    @Cacheable(value = CacheConfig.TEST_FOLDERS_CACHE, key = "'project:' + #projectId + ':parent:' + #parentId")
    public List<TestRepositoryFolderResponse> getFolders(UUID projectId, UUID parentId) {
        log.debug("Cache MISS - Fetching folders for project: {}", projectId);
        return testManagementService.getFolders(projectId, parentId);
    }

    @Cacheable(value = CacheConfig.ENVIRONMENTS_CACHE, key = "'project:' + #projectId")
    public List<TestEnvironmentResponse> getEnvironments(UUID projectId) {
        log.debug("Cache MISS - Fetching environments for project: {}", projectId);
        return testManagementService.getEnvironments(projectId);
    }

    @Cacheable(value = CacheConfig.TEST_SUMMARY_CACHE, key = "'project:' + #projectId")
    public TestSummaryResponse getTestSummary(UUID projectId) {
        log.debug("Cache MISS - Fetching test summary for project: {}", projectId);
        return testManagementService.getTestSummary(projectId);
    }

    @Cacheable(value = CacheConfig.TRACEABILITY_CACHE, key = "'project:' + #projectId")
    public TraceabilityMatrixResponse getTraceabilityMatrix(UUID projectId) {
        log.debug("Cache MISS - Fetching traceability matrix for project: {}", projectId);
        return testManagementService.getTraceabilityMatrix(projectId);
    }

    @Cacheable(value = CacheConfig.REQUIREMENT_COVERAGE_CACHE, key = "'req:' + #requirementKey")
    public RequirementCoverageResponse getRequirementCoverage(String requirementKey) {
        log.debug("Cache MISS - Fetching coverage for requirement: {}", requirementKey);
        return testManagementService.getRequirementCoverage(requirementKey);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.TESTS_CACHE, key = "#testId"),
            @CacheEvict(value = CacheConfig.TEST_SUMMARY_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.TRACEABILITY_CACHE, allEntries = true)
    })
    public void evictTestCaches(UUID testId) {
        log.debug("Evicting all caches related to test: {}", testId);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.TEST_SETS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.TEST_PLANS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.TEST_SUMMARY_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.REPORTS_CACHE, allEntries = true)
    })
    public void evictProjectCaches() {
        log.debug("Evicting all project-level caches");
    }
}