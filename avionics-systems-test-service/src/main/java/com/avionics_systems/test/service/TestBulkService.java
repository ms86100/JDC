package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.BulkTestResponse;
import com.avionics_systems.test.entity.TestIssue;
import com.avionics_systems.test.entity.TestSetItem;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.repository.TestIssueRepository;
import com.avionics_systems.test.repository.TestSetItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestBulkService {

    private final TestIssueRepository testIssueRepository;
    private final TestSetItemRepository testSetItemRepository;

    @Transactional
    public BulkTestResponse bulkUpdateStatus(List<UUID> testIds, String status) {
        log.info("Bulk updating status to '{}' for {} tests", status, testIds.size());
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        for (UUID testId : testIds) {
            try {
                TestIssue test = testIssueRepository.findById(testId)
                        .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
                test.setStatus(status);
                testIssueRepository.save(test);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("Test " + testId + ": " + e.getMessage());
                log.warn("Failed to update status for test {}: {}", testId, e.getMessage());
            }
        }

        return BulkTestResponse.builder()
                .totalRequested(testIds.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkTestResponse bulkAssign(List<UUID> testIds, UUID ownerId) {
        log.info("Bulk assigning {} tests to owner {}", testIds.size(), ownerId);
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        for (UUID testId : testIds) {
            try {
                TestIssue test = testIssueRepository.findById(testId)
                        .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
                test.setOwnerId(ownerId);
                testIssueRepository.save(test);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("Test " + testId + ": " + e.getMessage());
                log.warn("Failed to assign test {}: {}", testId, e.getMessage());
            }
        }

        return BulkTestResponse.builder()
                .totalRequested(testIds.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkTestResponse bulkMoveToFolder(List<UUID> testIds, UUID folderId) {
        log.info("Bulk moving {} tests to folder {}", testIds.size(), folderId);
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        for (UUID testId : testIds) {
            try {
                TestIssue test = testIssueRepository.findById(testId)
                        .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
                test.setFolderId(folderId);
                testIssueRepository.save(test);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("Test " + testId + ": " + e.getMessage());
                log.warn("Failed to move test {} to folder: {}", testId, e.getMessage());
            }
        }

        return BulkTestResponse.builder()
                .totalRequested(testIds.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkTestResponse bulkAddToTestSet(List<UUID> testIds, UUID testSetId) {
        log.info("Bulk adding {} tests to test set {}", testIds.size(), testSetId);
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        for (UUID testId : testIds) {
            try {
                if (testSetItemRepository.findByTestSetIdAndTestId(testSetId, testId).isPresent()) {
                    successCount++;
                    continue;
                }
                TestSetItem item = TestSetItem.builder()
                        .testSetId(testSetId)
                        .testId(testId)
                        .addedAt(LocalDateTime.now())
                        .build();
                testSetItemRepository.save(item);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("Test " + testId + ": " + e.getMessage());
                log.warn("Failed to add test {} to test set: {}", testId, e.getMessage());
            }
        }

        return BulkTestResponse.builder()
                .totalRequested(testIds.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkTestResponse bulkAddLabels(List<UUID> testIds, List<String> labels) {
        log.info("Bulk adding {} labels to {} tests", labels.size(), testIds.size());
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        for (UUID testId : testIds) {
            try {
                TestIssue test = testIssueRepository.findById(testId)
                        .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
                List<String> currentLabels = test.getLabels() != null
                        ? new ArrayList<>(test.getLabels())
                        : new ArrayList<>();
                for (String label : labels) {
                    if (!currentLabels.contains(label)) {
                        currentLabels.add(label);
                    }
                }
                test.setLabels(currentLabels);
                testIssueRepository.save(test);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("Test " + testId + ": " + e.getMessage());
                log.warn("Failed to add labels to test {}: {}", testId, e.getMessage());
            }
        }

        return BulkTestResponse.builder()
                .totalRequested(testIds.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkTestResponse bulkDelete(List<UUID> testIds) {
        log.info("Bulk soft-deleting {} tests", testIds.size());
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        for (UUID testId : testIds) {
            try {
                TestIssue test = testIssueRepository.findById(testId)
                        .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
                test.setArchived(true);
                testIssueRepository.save(test);
                successCount++;
            } catch (Exception e) {
                failedCount++;
                errors.add("Test " + testId + ": " + e.getMessage());
                log.warn("Failed to soft-delete test {}: {}", testId, e.getMessage());
            }
        }

        return BulkTestResponse.builder()
                .totalRequested(testIds.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .build();
    }
}
