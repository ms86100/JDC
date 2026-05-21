package com.jira.test.service;

import com.jira.test.dto.FolderStatsResponse;
import com.jira.test.entity.TestExecution;
import com.jira.test.entity.TestFolder;
import com.jira.test.entity.TestIssue;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderStatsService {

    private final TestFolderRepository folderRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;

    @Transactional(readOnly = true)
    public FolderStatsResponse getFolderStats(UUID folderId) {
        TestFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "id", folderId));

        return calculateFolderStats(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderStatsResponse> getAllFolderStats(UUID projectId) {
        List<TestFolder> folders = folderRepository.findByProjectIdOrderBySortOrderAsc(projectId);
        List<FolderStatsResponse> stats = new ArrayList<>();

        for (TestFolder folder : folders) {
            stats.add(calculateFolderStats(folder));
        }

        return stats;
    }

    private FolderStatsResponse calculateFolderStats(TestFolder folder) {
        // Get all test IDs directly in this folder
        List<TestIssue> testsInFolder = testIssueRepository.findByFolderId(folder.getId());
        int totalTests = testsInFolder.size();

        // Also count tests in child folders recursively
        List<UUID> allFolderIds = getAllChildFolderIds(folder.getId());
        for (UUID childId : allFolderIds) {
            totalTests += testIssueRepository.findByFolderId(childId).size();
        }

        // Calculate execution stats for tests in this folder
        int passedTests = 0;
        int failedTests = 0;
        int blockedTests = 0;
        int notRunTests = 0;

        for (TestIssue test : testsInFolder) {
            // Get latest execution for each test
            List<TestExecution> executions = executionRepository.findByTestIdOrderByCreatedAtDesc(test.getId());
            if (!executions.isEmpty()) {
                TestExecution latestExecution = executions.get(0);
                String status = latestExecution.getStatus();

                switch (status) {
                    case "PASSED":
                        passedTests++;
                        break;
                    case "FAILED":
                        failedTests++;
                        break;
                    case "BLOCKED":
                        blockedTests++;
                        break;
                    case "NOT_RUN":
                    case "RUNNING":
                    default:
                        notRunTests++;
                        break;
                }
            } else {
                notRunTests++;
            }
        }

        double passRate = 0.0;
        int executedTests = passedTests + failedTests + blockedTests;
        if (executedTests > 0) {
            passRate = (double) passedTests / executedTests * 100.0;
        }

        double executionProgress = 0.0;
        if (totalTests > 0) {
            executionProgress = (double) (totalTests - notRunTests) / totalTests * 100.0;
        }

        return FolderStatsResponse.builder()
                .folderId(folder.getId())
                .folderName(folder.getName())
                .projectId(folder.getProjectId())
                .totalTests(totalTests)
                .directTests(testsInFolder.size())
                .passedTests(passedTests)
                .failedTests(failedTests)
                .blockedTests(blockedTests)
                .notRunTests(notRunTests)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .executionProgress(Math.round(executionProgress * 100.0) / 100.0)
                .build();
    }

    private List<UUID> getAllChildFolderIds(UUID parentId) {
        List<UUID> allIds = new ArrayList<>();
        Queue<UUID> toProcess = new LinkedList<>();
        toProcess.add(parentId);

        while (!toProcess.isEmpty()) {
            UUID currentId = toProcess.poll();
            List<TestFolder> children = folderRepository.findByParentIdOrderBySortOrderAsc(currentId);

            for (TestFolder child : children) {
                allIds.add(child.getId());
                toProcess.add(child.getId());
            }
        }

        return allIds;
    }
}