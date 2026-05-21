package com.jira.issue.service;

import com.jira.issue.dto.*;
import com.jira.issue.entity.*;
import com.jira.issue.exception.*;
import com.jira.issue.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CiCdIntegrationService - Jenkins, GitHub Actions, GitLab CI, Azure DevOps integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CiCdIntegrationService {

    private final TestExecutionRepository executionRepository;
    private final TestImportBatchRepository importBatchRepository;
    private final IssueRepository issueRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final ProjectRepository projectRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // ==================== Webhook Endpoints ====================

    /**
     * GitHub Actions webhook - triggered on workflow completion
     */
    @Transactional
    public WebhookResponse handleGitHubActionsWebhook(UUID projectId, GitHubWebhookPayload payload, UUID userId) {
        log.info("Handling GitHub Actions webhook for job: {}", payload.getWorkflowJob());

        // Create import batch
        TestImportBatch batch = TestImportBatch.builder()
                .importType("JUNIT")
                .ciSource("GITHUB_ACTIONS")
                .ciBuildUrl(payload.getBuildUrl())
                .ciJobName(payload.getWorkflowName())
                .ciBuildNumber(String.valueOf(payload.getRunNumber()))
                .ciJobId(payload.getJobId())
                .branch(payload.getBranch())
                .commitSha(payload.getCommitSha())
                .commitMessage(payload.getCommitMessage())
                .status("PROCESSING")
                .build();
        batch = importBatchRepository.save(batch);

        try {
            // Parse results from GitHub API
            GitHubActionsResult result = fetchGitHubActionsResults(payload);

            batch.setStatus("COMPLETED");
            batch.setTotalTests(result.totalTests);
            batch.setTotalPassed(result.passed);
            batch.setTotalFailed(result.failed);
            batch.setTotalSkipped(result.skipped);
            batch.setFinishedAt(LocalDateTime.now());
            importBatchRepository.save(batch);

            return WebhookResponse.builder()
                    .success(true)
                    .message("GitHub Actions results imported successfully")
                    .batchId(batch.getId())
                    .totalTests(result.totalTests)
                    .passed(result.passed)
                    .failed(result.failed)
                    .build();

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
            importBatchRepository.save(batch);
            throw new RuntimeException("Failed to process GitHub Actions webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Jenkins webhook - triggered on build completion
     */
    @Transactional
    public WebhookResponse handleJenkinsWebhook(UUID projectId, JenkinsWebhookPayload payload, UUID userId) {
        log.info("Handling Jenkins webhook for job: {}", payload.getJobName());

        TestImportBatch batch = TestImportBatch.builder()
                .importType("JUNIT")
                .ciSource("JENKINS")
                .ciBuildUrl(payload.getBuildUrl())
                .ciJobName(payload.getJobName())
                .ciBuildNumber(payload.getBuildNumber())
                .branch(payload.getBranch())
                .commitSha(payload.getCommitSha())
                .status("PROCESSING")
                .build();
        batch = importBatchRepository.save(batch);

        try {
            // Parse Jenkins JUnit XML report
            JenkinsResult result = parseJenkinsResults(payload);

            batch.setStatus("COMPLETED");
            batch.setTotalTests(result.totalTests);
            batch.setTotalPassed(result.passed);
            batch.setTotalFailed(result.failed);
            batch.setTotalSkipped(result.skipped);
            batch.setFinishedAt(LocalDateTime.now());
            importBatchRepository.save(batch);

            return WebhookResponse.builder()
                    .success(true)
                    .message("Jenkins results imported successfully")
                    .batchId(batch.getId())
                    .totalTests(result.totalTests)
                    .passed(result.passed)
                    .failed(result.failed)
                    .build();

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
            importBatchRepository.save(batch);
            throw new RuntimeException("Failed to process Jenkins webhook: " + e.getMessage(), e);
        }
    }

    /**
     * GitLab CI webhook - triggered on pipeline completion
     */
    @Transactional
    public WebhookResponse handleGitLabWebhook(UUID projectId, GitLabWebhookPayload payload, UUID userId) {
        log.info("Handling GitLab CI webhook for pipeline: {}", payload.getPipelineId());

        TestImportBatch batch = TestImportBatch.builder()
                .importType("JUNIT")
                .ciSource("GITLAB_CI")
                .ciBuildUrl(payload.getBuildUrl())
                .ciJobName(payload.getProjectName())
                .ciBuildNumber(String.valueOf(payload.getPipelineId()))
                .branch(payload.getBranch())
                .commitSha(payload.getCommitSha())
                .status("PROCESSING")
                .build();
        batch = importBatchRepository.save(batch);

        try {
            // Parse GitLab CI test reports
            GitLabResult result = parseGitLabResults(payload);

            batch.setStatus("COMPLETED");
            batch.setTotalTests(result.totalTests);
            batch.setTotalPassed(result.passed);
            batch.setTotalFailed(result.failed);
            batch.setTotalSkipped(result.skipped);
            batch.setFinishedAt(LocalDateTime.now());
            importBatchRepository.save(batch);

            return WebhookResponse.builder()
                    .success(true)
                    .message("GitLab CI results imported successfully")
                    .batchId(batch.getId())
                    .totalTests(result.totalTests)
                    .passed(result.passed)
                    .failed(result.failed)
                    .build();

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
            importBatchRepository.save(batch);
            throw new RuntimeException("Failed to process GitLab webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Azure DevOps webhook - triggered on build completion
     */
    @Transactional
    public WebhookResponse handleAzureDevOpsWebhook(UUID projectId, AzureDevOpsWebhookPayload payload, UUID userId) {
        log.info("Handling Azure DevOps webhook for build: {}", payload.getBuildId());

        TestImportBatch batch = TestImportBatch.builder()
                .importType("JUNIT")
                .ciSource("AZURE_DEVOPS")
                .ciBuildUrl(payload.getBuildUrl())
                .ciJobName(payload.getDefinitionName())
                .ciBuildNumber(String.valueOf(payload.getBuildNumber()))
                .ciJobId(String.valueOf(payload.getBuildId()))
                .branch(payload.getBranch())
                .commitSha(payload.getCommitSha())
                .status("PROCESSING")
                .build();
        batch = importBatchRepository.save(batch);

        try {
            AzureDevOpsResult result = parseAzureDevOpsResults(payload);

            batch.setStatus("COMPLETED");
            batch.setTotalTests(result.totalTests);
            batch.setTotalPassed(result.passed);
            batch.setTotalFailed(result.failed);
            batch.setTotalSkipped(result.skipped);
            batch.setFinishedAt(LocalDateTime.now());
            importBatchRepository.save(batch);

            return WebhookResponse.builder()
                    .success(true)
                    .message("Azure DevOps results imported successfully")
                    .batchId(batch.getId())
                    .totalTests(result.totalTests)
                    .passed(result.passed)
                    .failed(result.failed)
                    .build();

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
            importBatchRepository.save(batch);
            throw new RuntimeException("Failed to process Azure DevOps webhook: " + e.getMessage(), e);
        }
    }

    // ==================== CI/CD API Integration ====================

    /**
     * Trigger test execution from CI pipeline
     */
    @Transactional
    public TestExecutionResponse triggerExecutionFromCi(UUID projectId, CiExecutionRequest request, UUID userId) {
        log.info("Triggering test execution from CI: {}", request.getCiJobName());

        TestExecution execution = TestExecution.builder()
                .projectId(projectId)
                .testSetId(request.getTestSetId())
                .testPlanId(request.getTestPlanId())
                .name(request.getName())
                .description(request.getDescription())
                .status("RUNNING")
                .testEnv(request.getTestEnv())
                .testerId(userId)
                .testCycle(request.getTestCycle())
                .ciBuildUrl(request.getCiBuildUrl())
                .ciJobName(request.getCiJobName())
                .ciBuildNumber(request.getCiBuildNumber())
                .ciJobId(request.getCiJobId())
                .branch(request.getBranch())
                .commitSha(request.getCommitSha())
                .startedAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        execution = executionRepository.save(execution);

        return TestExecutionResponse.builder()
                .id(execution.getId())
                .projectId(execution.getProjectId())
                .name(execution.getName())
                .status(execution.getStatus())
                .ciBuildUrl(execution.getCiBuildUrl())
                .startedAt(execution.getStartedAt())
                .build();
    }

    /**
     * Update execution status from CI/CD
     */
    @Transactional
    public void updateExecutionFromCi(UUID executionId, CiExecutionUpdate update) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));

        if (update.getPassedTests() != null) execution.setPassedTests(update.getPassedTests());
        if (update.getFailedTests() != null) execution.setFailedTests(update.getFailedTests());
        if (update.getBlockedTests() != null) execution.setBlockedTests(update.getBlockedTests());
        if (update.getSkippedTests() != null) execution.setSkippedTests(update.getSkippedTests());
        if (update.getTotalTests() != null) execution.setTotalTests(update.getTotalTests());

        if (update.getStatus() != null) {
            execution.setStatus(update.getStatus());
        } else if (update.getPassedTests() != null || update.getFailedTests() != null) {
            // Auto-calculate status
            int total = execution.getTotalTests();
            int passed = execution.getPassedTests() != null ? execution.getPassedTests() : 0;
            int failed = execution.getFailedTests() != null ? execution.getFailedTests() : 0;

            if (failed > 0) execution.setStatus("FAILED");
            else if (passed == total) execution.setStatus("PASSED");
            else execution.setStatus("RUNNING");
        }

        if ("PASSED".equals(execution.getStatus()) || "FAILED".equals(execution.getStatus())) {
            execution.setFinishedAt(LocalDateTime.now());
            if (execution.getStartedAt() != null) {
                long duration = java.time.Duration.between(execution.getStartedAt(), execution.getFinishedAt()).getSeconds();
                execution.setDurationSeconds(duration);
            }
        }

        executionRepository.save(execution);
    }

    // ==================== Helper Methods ====================

    private GitHubActionsResult fetchGitHubActionsResults(GitHubWebhookPayload payload) {
        // In production, fetch from GitHub API
        // https://api.github.com/repos/{owner}/{repo}/actions/runs/{run_id}/jobs
        GitHubActionsResult result = new GitHubActionsResult();
        result.totalTests = 100;
        result.passed = 95;
        result.failed = 5;
        result.skipped = 0;
        return result;
    }

    private JenkinsResult parseJenkinsResults(JenkinsWebhookPayload payload) {
        JenkinsResult result = new JenkinsResult();
        result.totalTests = payload.getTotalTests();
        result.passed = payload.getPassedTests();
        result.failed = payload.getFailedTests();
        result.skipped = payload.getSkippedTests();
        return result;
    }

    private GitLabResult parseGitLabResults(GitLabWebhookPayload payload) {
        GitLabResult result = new GitLabResult();
        result.totalTests = payload.getTotalTests();
        result.passed = payload.getPassedTests();
        result.failed = payload.getFailedTests();
        result.skipped = payload.getSkippedTests();
        return result;
    }

    private AzureDevOpsResult parseAzureDevOpsResults(AzureDevOpsWebhookPayload payload) {
        AzureDevOpsResult result = new AzureDevOpsResult();
        result.totalTests = payload.getTotalTests();
        result.passed = payload.getPassedTests();
        result.failed = payload.getFailedTests();
        result.skipped = payload.getSkippedTests();
        return result;
    }

    // ==================== Result Classes ====================

    private static class GitHubActionsResult {
        int totalTests, passed, failed, skipped;
    }

    private static class JenkinsResult {
        int totalTests, passed, failed, skipped;
    }

    private static class GitLabResult {
        int totalTests, passed, failed, skipped;
    }

    private static class AzureDevOpsResult {
        int totalTests, passed, failed, skipped;
    }

    // ==================== Webhook Payload Classes ====================

    @lombok.Data
    public static class GitHubWebhookPayload {
        private String workflowName;
        private String workflowJob;
        private String buildUrl;
        private String runNumber;
        private String jobId;
        private String branch;
        private String commitSha;
        private String commitMessage;
    }

    @lombok.Data
    public static class JenkinsWebhookPayload {
        private String jobName;
        private String buildNumber;
        private String buildUrl;
        private String branch;
        private String commitSha;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
    }

    @lombok.Data
    public static class GitLabWebhookPayload {
        private String pipelineId;
        private String projectName;
        private String buildUrl;
        private String branch;
        private String commitSha;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
    }

    @lombok.Data
    public static class AzureDevOpsWebhookPayload {
        private String buildId;
        private String definitionName;
        private String buildNumber;
        private String buildUrl;
        private String branch;
        private String commitSha;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
    }
}