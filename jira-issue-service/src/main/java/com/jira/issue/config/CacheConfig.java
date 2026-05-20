package com.jira.issue.config;

/**
 * Cache configuration constants
 * Phase 13 - Redis Caching Layer
 */
public class CacheConfig {

    public static final String TESTS_CACHE = "tests";
    public static final String TEST_SETS_CACHE = "testSets";
    public static final String TEST_PLANS_CACHE = "testPlans";
    public static final String TEST_EXECUTIONS_CACHE = "testExecutions";
    public static final String TEST_FOLDERS_CACHE = "testFolders";
    public static final String ENVIRONMENTS_CACHE = "environments";
    public static final String TRACEABILITY_CACHE = "traceability";
    public static final String REPORTS_CACHE = "reports";
    public static final String PROJECT_CACHE = "projects";
    public static final String TEST_SUMMARY_CACHE = "testSummary";
    public static final String REQUIREMENT_COVERAGE_CACHE = "requirementCoverage";

    private CacheConfig() {
        // Prevent instantiation
    }
}
