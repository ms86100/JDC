package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TraceabilityService {

    private final RequirementLinkRepository requirementLinkRepository;
    private final TestIssueRepository testIssueRepository;
    private final DefectLinkRepository defectLinkRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;

    @Transactional
    public RequirementLinkResponse linkRequirementToTest(RequirementLinkRequest request) {
        log.info("Linking requirement {} to test {}", request.getRequirementKey(), request.getTestId());

        if (requirementLinkRepository.existsByRequirementKeyAndTestId(request.getRequirementKey(), request.getTestId())) {
            throw new DuplicateResourceException("Requirement " + request.getRequirementKey() + " already linked to test " + request.getTestId());
        }

        RequirementLink link = RequirementLink.builder()
                .requirementKey(request.getRequirementKey())
                .requirementType(request.getRequirementType())
                .testId(request.getTestId())
                .coverageStatus("COVERED")
                .build();

        link = requirementLinkRepository.save(link);

        log.info("Requirement linked with id: {}", link.getId());
        return mapToRequirementLinkResponse(link);
    }

    @Transactional(readOnly = true)
    public TraceabilityMatrixResponse getTraceabilityMatrix(UUID projectId) {
        log.info("Getting traceability matrix for project: {}", projectId);

        List<TestIssue> projectTests = testIssueRepository.findByProjectIdAndArchivedFalse(projectId);
        List<RequirementLink> allLinks = requirementLinkRepository.findAll();

        List<String> allRequirementKeys = allLinks.stream()
                .map(RequirementLink::getRequirementKey)
                .distinct()
                .sorted()
                .toList();

        List<TraceabilityMatrixResponse.RequirementRow> rows = new ArrayList<>();

        for (String reqKey : allRequirementKeys) {
            List<RequirementLink> reqLinks = allLinks.stream()
                    .filter(l -> l.getRequirementKey().equals(reqKey))
                    .toList();

            List<UUID> testIds = reqLinks.stream()
                    .map(RequirementLink::getTestId)
                    .toList();

            List<TestIssue> coveredTests = projectTests.stream()
                    .filter(t -> testIds.contains(t.getId()))
                    .toList();

            List<TraceabilityMatrixResponse.TestCoverage> testCoverages = new ArrayList<>();

            for (TestIssue test : coveredTests) {
                String lastStatus = getLastExecutionStatus(test.getId());
                int passRate = getExecutionPassRate(test.getId());

                testCoverages.add(TraceabilityMatrixResponse.TestCoverage.builder()
                        .testId(test.getId())
                        .testName(test.getName())
                        .status(test.getStatus())
                        .lastExecutionStatus(lastStatus)
                        .executionPassRate(passRate)
                        .build());
            }

            rows.add(TraceabilityMatrixResponse.RequirementRow.builder()
                    .requirementKey(reqKey)
                    .requirementType(reqLinks.isEmpty() ? null : reqLinks.get(0).getRequirementType())
                    .testCount(testCoverages.size())
                    .tests(testCoverages)
                    .build());
        }

        return TraceabilityMatrixResponse.builder()
                .requirements(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TestResponse> getCoverageForRequirement(String requirementKey) {
        log.info("Getting coverage for requirement: {}", requirementKey);

        List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(requirementKey);
        List<UUID> testIds = links.stream()
                .map(RequirementLink::getTestId)
                .toList();

        return testIssueRepository.findAll().stream()
                .filter(t -> testIds.contains(t.getId()))
                .map(this::mapToTestResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DefectLinkResponse> getDefectsForTest(UUID testId) {
        log.info("Getting defects for test: {}", testId);

        List<DefectLink> defectLinks = defectLinkRepository.findAll().stream()
                .filter(dl -> dl.getStepResultId() != null)
                .filter(dl -> {
                    Optional<TestIssue> test = testIssueRepository.findById(testId);
                    return test.isPresent() && testId.equals(test.get().getId());
                })
                .toList();

        return defectLinks.stream()
                .map(this::mapToDefectLinkResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DefectLinkResponse linkDefect(UUID executionId, UUID stepResultId, String defectKey, String severity) {
        log.info("Linking defect {} to execution {}", defectKey, executionId);

        TestExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("TestExecution", "id", executionId));

        StepResult stepResult = stepResultRepository.findById(stepResultId)
                .orElseThrow(() -> new ResourceNotFoundException("StepResult", "id", stepResultId));

        DefectLink defectLink = DefectLink.builder()
                .executionId(executionId)
                .stepResultId(stepResultId)
                .defectKey(defectKey)
                .severity(severity != null ? severity : "MEDIUM")
                .status("OPEN")
                .build();

        defectLink = defectLinkRepository.save(defectLink);

        log.info("Defect linked with id: {}", defectLink.getId());
        return mapToDefectLinkResponse(defectLink);
    }

    private String getLastExecutionStatus(UUID testId) {
        List<TestExecution> executions = executionRepository.findByTestId(testId);
        if (executions.isEmpty()) return null;
        executions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return executions.get(0).getStatus();
    }

    private int getExecutionPassRate(UUID testId) {
        List<TestExecution> executions = executionRepository.findByTestId(testId);
        if (executions.isEmpty()) return 0;

        long totalPassed = executions.stream()
                .filter(e -> "PASSED".equals(e.getStatus()))
                .count();
        return (int) ((totalPassed * 100) / executions.size());
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
                .requirementKeys(test.getRequirementKeys())
                .gherkinFeatureKey(test.getGherkinFeatureKey())
                .gherkinScenarioId(test.getGherkinScenarioId())
                .testSetId(test.getTestSetId())
                .archived(test.getArchived())
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }

    private RequirementLinkResponse mapToRequirementLinkResponse(RequirementLink link) {
        return RequirementLinkResponse.builder()
                .id(link.getId())
                .requirementKey(link.getRequirementKey())
                .requirementType(link.getRequirementType())
                .testId(link.getTestId())
                .coverageStatus(link.getCoverageStatus())
                .createdAt(link.getCreatedAt())
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
