package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestExecutionService {

    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;
    private final DefectLinkRepository defectLinkRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestStepRepository testStepRepository;

    @Transactional
    public TestExecutionResponse createExecution(CreateExecutionRequest request) {
        log.info("Creating test execution: {}", request.getName());

        TestExecution execution = TestExecution.builder()
                .testPlanId(request.getTestPlanId())
                .testSetId(request.getTestSetId())
                .testId(request.getTestId())
                .name(request.getName())
                .description(request.getDescription())
                .testEnv(request.getTestEnv())
                .testerId(request.getTesterId())
                .testCycle(request.getTestCycle())
                .ciBuildUrl(request.getCiBuildUrl())
                .ciJobId(request.getCiJobId())
                .status("RUNNING")
                .startedAt(LocalDateTime.now())
                .totalTests(request.getStepResults() != null ? request.getStepResults().size() : 0)
                .build();

        execution = executionRepository.save(execution);

        if (request.getStepResults() != null && !request.getStepResults().isEmpty()) {
            for (CreateExecutionRequest.StepResultDto stepDto : request.getStepResults()) {
                StepResult stepResult = StepResult.builder()
                        .executionId(execution.getId())
                        .stepId(stepDto.getStepId())
                        .status(stepDto.getStatus() != null ? stepDto.getStatus() : "NOT_RUN")
                        .actualResult(stepDto.getActualResult())
                        .evidenceUrls(stepDto.getEvidenceUrls())
                        .defectKey(stepDto.getDefectKey())
                        .comment(stepDto.getComment())
                        .executedAt(LocalDateTime.now())
                        .build();
                stepResultRepository.save(stepResult);
            }
        }

        log.info("Test execution created with id: {}", execution.getId());
        return mapToExecutionResponse(execution);
    }

    @Transactional(readOnly = true)
    public TestExecutionResponse getExecution(UUID executionId) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));
        return mapToExecutionResponse(execution);
    }

    @Transactional(readOnly = true)
    public List<TestExecutionResponse> getExecutionHistory(UUID testerId, LocalDateTime since) {
        if (testerId != null && since != null) {
            return executionRepository.findRecentByTester(testerId, since).stream()
                    .map(this::mapToExecutionResponse)
                    .collect(Collectors.toList());
        } else if (testerId != null) {
            return executionRepository.findByTesterId(testerId).stream()
                    .map(this::mapToExecutionResponse)
                    .collect(Collectors.toList());
        } else if (since != null) {
            return executionRepository.findAll().stream()
                    .filter(e -> e.getCreatedAt().isAfter(since))
                    .map(this::mapToExecutionResponse)
                    .collect(Collectors.toList());
        }
        return executionRepository.findAll().stream()
                .map(this::mapToExecutionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StepResultResponse updateStepResult(UUID executionId, UUID stepId, StepResultUpdateRequest request) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));

        StepResult stepResult = stepResultRepository.findByExecutionId(executionId).stream()
                .filter(s -> s.getStepId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("StepResult", "id", stepId));

        stepResult.setStatus(request.getStatus());
        stepResult.setActualResult(request.getActualResult());
        if (request.getEvidenceUrls() != null) {
            stepResult.setEvidenceUrls(request.getEvidenceUrls());
        }
        stepResult.setDefectKey(request.getDefectKey());
        stepResult.setComment(request.getComment());
        stepResult.setExecutedAt(LocalDateTime.now());

        stepResult = stepResultRepository.save(stepResult);

        updateExecutionCounts(execution);

        log.info("Step result updated for execution: {}, step: {}", executionId, stepId);
        return mapToStepResultResponse(stepResult);
    }

    @Transactional
    public TestExecutionResponse completeExecution(UUID executionId, String finalStatus) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));

        execution.setStatus(finalStatus != null ? finalStatus : calculateFinalStatus(execution));
        execution.setFinishedAt(LocalDateTime.now());

        execution = executionRepository.save(execution);
        final UUID testId = execution.getTestId();
        final String execStatus = execution.getStatus();

        if (testId != null) {
            testIssueRepository.findById(testId).ifPresent(test -> {
                test.setStatus(mapStatus(execStatus));
                testIssueRepository.save(test);
            });
        }

        log.info("Test execution completed: {} with status: {}", executionId, execution.getStatus());
        return mapToExecutionResponse(execution);
    }

    @Transactional
    public StepResultResponse addEvidence(UUID executionId, UUID stepId, List<String> evidenceUrls) {
        StepResult stepResult = stepResultRepository.findByExecutionId(executionId).stream()
                .filter(s -> stepId == null || s.getStepId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("StepResult", "id", stepId));

        List<String> existing = stepResult.getEvidenceUrls() != null
                ? new ArrayList<>(stepResult.getEvidenceUrls())
                : new ArrayList<>();
        existing.addAll(evidenceUrls);
        stepResult.setEvidenceUrls(existing);

        stepResult = stepResultRepository.save(stepResult);

        log.info("Evidence added to step result: {}", stepResult.getId());
        return mapToStepResultResponse(stepResult);
    }

    @Transactional
    public DefectLinkResponse linkDefect(UUID executionId, UUID stepResultId, String defectKey, String severity) {
        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));

        StepResult stepResult = stepResultRepository.findById(stepResultId)
                .orElseThrow(() -> new ResourceNotFoundException("StepResult", "id", stepResultId));

        DefectLink defectLink = DefectLink.builder()
                .executionId(execution.getId())
                .stepResultId(stepResultId)
                .defectKey(defectKey)
                .severity(severity != null ? severity : "MEDIUM")
                .status("OPEN")
                .build();

        defectLink = defectLinkRepository.save(defectLink);

        stepResult.setDefectKey(defectKey);
        stepResultRepository.save(stepResult);

        log.info("Defect linked: {} to execution: {}", defectKey, executionId);
        return mapToDefectLinkResponse(defectLink);
    }

    private void updateExecutionCounts(TestExecution execution) {
        List<StepResult> results = stepResultRepository.findByExecutionId(execution.getId());

        long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        long blocked = results.stream().filter(r -> "BLOCKED".equals(r.getStatus())).count();
        long notRun = results.stream().filter(r -> "NOT_RUN".equals(r.getStatus())).count();

        execution.setPassedTests((int) passed);
        execution.setFailedTests((int) failed);
        execution.setBlockedTests((int) blocked);
        execution.setNotRunTests((int) notRun);

        if (passed + failed + blocked + notRun > 0 && notRun == 0) {
            execution.setStatus(calculateFinalStatus(execution));
            execution.setFinishedAt(LocalDateTime.now());
        }

        executionRepository.save(execution);
    }

    private String calculateFinalStatus(TestExecution execution) {
        if (execution.getFailedTests() > 0) return "FAILED";
        if (execution.getBlockedTests() > 0) return "BLOCKED";
        if (execution.getPassedTests() > 0 && execution.getNotRunTests() == 0) return "PASSED";
        if (execution.getNotRunTests() > 0) return "RUNNING";
        return "CANCELLED";
    }

    private String mapStatus(String executionStatus) {
        return switch (executionStatus) {
            case "PASSED" -> "PASS";
            case "FAILED" -> "FAIL";
            case "BLOCKED" -> "BLOCKED";
            default -> executionStatus;
        };
    }

    private TestExecutionResponse mapToExecutionResponse(TestExecution execution) {
        return TestExecutionResponse.builder()
                .id(execution.getId())
                .testPlanId(execution.getTestPlanId())
                .testSetId(execution.getTestSetId())
                .testId(execution.getTestId())
                .name(execution.getName())
                .description(execution.getDescription())
                .status(execution.getStatus())
                .testEnv(execution.getTestEnv())
                .testerId(execution.getTesterId())
                .testCycle(execution.getTestCycle())
                .ciBuildUrl(execution.getCiBuildUrl())
                .totalTests(execution.getTotalTests())
                .passedTests(execution.getPassedTests())
                .failedTests(execution.getFailedTests())
                .blockedTests(execution.getBlockedTests())
                .notRunTests(execution.getNotRunTests())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .createdAt(execution.getCreatedAt())
                .build();
    }

    private StepResultResponse mapToStepResultResponse(StepResult stepResult) {
        return StepResultResponse.builder()
                .id(stepResult.getId())
                .executionId(stepResult.getExecutionId())
                .stepId(stepResult.getStepId())
                .status(stepResult.getStatus())
                .actualResult(stepResult.getActualResult())
                .evidenceUrls(stepResult.getEvidenceUrls())
                .defectKey(stepResult.getDefectKey())
                .comment(stepResult.getComment())
                .executedAt(stepResult.getExecutedAt())
                .createdAt(stepResult.getCreatedAt())
                .build();
    }

    private DefectLinkResponse mapToDefectLinkResponse(DefectLink defectLink) {
        return DefectLinkResponse.builder()
                .id(defectLink.getId())
                .defectKey(defectLink.getDefectKey())
                .executionId(defectLink.getExecutionId())
                .stepResultId(defectLink.getStepResultId())
                .severity(defectLink.getSeverity())
                .status(defectLink.getStatus())
                .createdAt(defectLink.getCreatedAt())
                .build();
    }
}
