package com.jira.issue.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for jira-issue-service.
 * Uses Caffeine for in-memory caching with configurable TTLs.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ISSUE_TYPE_CACHE = "issueTypeCache";
    public static final String COMPONENT_CACHE = "componentCache";
    public static final String VERSION_CACHE = "versionCache";
    public static final String LABEL_CACHE = "labelCache";
    public static final String EPIC_CACHE = "epicCache";
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

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                ISSUE_TYPE_CACHE,
                COMPONENT_CACHE,
                VERSION_CACHE,
                LABEL_CACHE,
                EPIC_CACHE,
                TESTS_CACHE,
                TEST_SETS_CACHE,
                TEST_PLANS_CACHE,
                TEST_EXECUTIONS_CACHE,
                TEST_FOLDERS_CACHE,
                ENVIRONMENTS_CACHE,
                TRACEABILITY_CACHE,
                REPORTS_CACHE,
                PROJECT_CACHE,
                TEST_SUMMARY_CACHE,
                REQUIREMENT_COVERAGE_CACHE
        );
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .expireAfterAccess(3, TimeUnit.MINUTES)
                .recordStats();
    }
}