package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.*;
import com.avionics_systems.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class XrayApiMappingService {

    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;

    private static final Map<String, String> STATUS_MAP = Map.of(
            "PASSED", "PASSED",
            "PASS", "PASSED",
            "FAILED", "FAILED",
            "FAIL", "FAILED",
            "TODO", "PENDING",
            "EXECUTING", "IN_PROGRESS",
            "ABORTED", "CANCELLED"
    );

    @Transactional
    public XrayImportResponse importExecution(XrayImportExecutionRequest request, UUID projectId) {
        log.info("Importing Xray execution for project: {}", projectId);

        XrayImportExecutionRequest.XrayExecutionInfo info = request.getInfo();
        String summary = info != null && info.getSummary() != null
                ? info.getSummary()
                : "Xray Execution Import";
        String description = info != null ? info.getDescription() : null;

        int passed = 0;
        int failed = 0;
        int total = 0;
        int skipped = 0;

        TestExecution execution = TestExecution.builder()
                .projectId(projectId)
                .name(summary)
                .description(description)
                .status("RUNNING")
                .testEnv(info != null && info.getTestEnvironments() != null && !info.getTestEnvironments().isEmpty()
                        ? String.join(",", info.getTestEnvironments()).substring(0, Math.min(50, String.join(",", info.getTestEnvironments()).length()))
                        : null)
                .startedAt(LocalDateTime.now())
                .build();

        execution = executionRepository.save(execution);

        if (request.getTests() != null) {
            for (XrayImportExecutionRequest.XrayTestResult testResult : request.getTests()) {
                total++;
                try {
                    TestIssue testIssue = findOrCreateTestByKey(testResult.getTestKey(), projectId);

                    String mappedStatus = mapXrayStatus(testResult.getStatus());
                    switch (mappedStatus) {
                        case "PASSED" -> passed++;
                        case "FAILED" -> failed++;
                        default -> skipped++;
                    }

                    if (testResult.getSteps() != null) {
                        int stepOrder = 1;
                        for (XrayImportExecutionRequest.XrayStepResult xrayStep : testResult.getSteps()) {
                            StepResult stepResult = StepResult.builder()
                                    .executionId(execution.getId())
                                    .stepId(testIssue.getId())
                                    .status(mapXrayStatus(xrayStep.getStatus()))
                                    .actualResult(xrayStep.getActualResult())
                                    .comment(xrayStep.getComment())
                                    .executedAt(LocalDateTime.now())
                                    .build();
                            stepResultRepository.save(stepResult);
                            stepOrder++;
                        }
                    } else {
                        StepResult stepResult = StepResult.builder()
                                .executionId(execution.getId())
                                .stepId(testIssue.getId())
                                .status(mappedStatus)
                                .comment(testResult.getComment())
                                .defectKey(testResult.getDefects() != null && !testResult.getDefects().isEmpty()
                                        ? String.join(",", testResult.getDefects())
                                        : null)
                                .executedAt(LocalDateTime.now())
                                .build();
                        stepResultRepository.save(stepResult);
                    }
                } catch (Exception e) {
                    log.warn("Failed to process Xray test result for key '{}': {}",
                            testResult.getTestKey(), e.getMessage());
                }
            }
        }

        execution.setTotalTests(total);
        execution.setPassedTests(passed);
        execution.setFailedTests(failed);
        execution.setNotRunTests(skipped);
        execution.setBlockedTests(0);
        execution.setStatus(failed > 0 ? "FAILED" : "PASSED");
        execution.setFinishedAt(LocalDateTime.now());
        executionRepository.save(execution);

        log.info("Xray execution import completed: {} total, {} passed, {} failed, {} other",
                total, passed, failed, skipped);

        return XrayImportResponse.builder()
                .testExecIssue(request.getTestExecutionKey())
                .id(execution.getId().toString())
                .key(request.getTestExecutionKey() != null ? request.getTestExecutionKey() : execution.getId().toString())
                .self("/api/raven/1.0/testexec/" + execution.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public TestResponse getTestByKey(String testKey, UUID projectId) {
        TestIssue test = testIssueRepository.findByProjectIdAndName(projectId, testKey)
                .orElseThrow(() -> new com.avionics_systems.test.exception.ResourceNotFoundException("Test", "key", testKey));
        return mapToTestResponse(test);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getExecutionSummary(UUID testExecId, UUID projectId) {
        TestExecution execution = executionRepository.findById(testExecId)
                .orElseThrow(() -> new com.avionics_systems.test.exception.ResourceNotFoundException("TestExecution", "id", testExecId));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", execution.getId());
        summary.put("name", execution.getName());
        summary.put("status", execution.getStatus());
        summary.put("totalTests", execution.getTotalTests());
        summary.put("passedTests", execution.getPassedTests());
        summary.put("failedTests", execution.getFailedTests());
        summary.put("notRunTests", execution.getNotRunTests());
        summary.put("startedAt", execution.getStartedAt());
        summary.put("finishedAt", execution.getFinishedAt());
        return summary;
    }

    private TestIssue findOrCreateTestByKey(String testKey, UUID projectId) {
        return testIssueRepository.findByProjectIdAndName(projectId, testKey)
                .orElseGet(() -> {
                    TestIssue newTest = TestIssue.builder()
                            .projectId(projectId)
                            .name(testKey)
                            .description("Auto-created from Xray import: " + testKey)
                            .testType("AUTOMATED")
                            .labels(List.of("automated", "xray-import"))
                            .build();
                    return testIssueRepository.save(newTest);
                });
    }

    private String mapXrayStatus(String xrayStatus) {
        if (xrayStatus == null) return "PENDING";
        return STATUS_MAP.getOrDefault(xrayStatus.toUpperCase(), "PENDING");
    }

    private TestResponse mapToTestResponse(TestIssue test) {
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
                .archived(test.getArchived())
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }
}
