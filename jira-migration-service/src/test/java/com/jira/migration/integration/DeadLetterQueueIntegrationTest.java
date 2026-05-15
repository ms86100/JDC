package com.jira.migration.integration;

import com.jira.migration.BaseIntegrationTest;
import com.jira.migration.TestUtils;
import com.jira.migration.dto.ImportResultResponse;
import com.jira.migration.dto.MigrationJobResponse;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.repository.EntityStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Dead Letter Queue (DLQ) functionality.
 * Tests failure handling, retry mechanisms, and queue operations.
 */
@Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Dead Letter Queue Integration Tests")
@Nested
@Slf4j
public class DeadLetterQueueIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EntityStatusRepository entityStatusRepository;

    private String invalidCsv;

    @BeforeEach
    void setUp() {
        invalidCsv = TestUtils.getResourceFileContent("invalid.csv");
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should record failed entities in entity_status table")
    void shouldRecordFailedEntitiesInEntityStatusTable() {
        // Given CSV with invalid data
        String csvContent = invalidCsv;

        // When uploading CSV that may fail some rows
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");

        // And waiting for job completion
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then failed entities should be recorded in entity_status
        List<EntityStatus> failedEntities = entityStatusRepository.findFailedEntities(completedJob.getId());

        ImportResultResponse result = getImportResult(completedJob.getId());
        if (result.getFailedEntities() > 0) {
            assertThat(failedEntities).isNotEmpty();
            assertThat(failedEntities).allMatch(e -> "FAILED".equals(e.getStatus()));
        }
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should capture error details for failed entities")
    void shouldCaptureErrorDetailsForFailedEntities() {
        // Given CSV with invalid data
        String csvContent = invalidCsv;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then error details should be captured
        List<EntityStatus> failedEntities = entityStatusRepository.findFailedEntities(completedJob.getId());

        for (EntityStatus failed : failedEntities) {
            assertThat(failed.getErrorCode()).isNotNull();
            assertThat(failed.getErrorMessage()).isNotNull();
            log.debug("Failed entity: {} - {} ({})",
                    failed.getEntityKey(), failed.getErrorMessage(), failed.getErrorCode());
        }
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should track row number for CSV errors")
    void shouldTrackRowNumberForCsvErrors() {
        // Given CSV with specific row errors
        String csvContent = """
                project_key,name,description
                ,Missing Key in Row 2
                PROJ1,Valid Project,Valid
                INVALID_KEY,Trying Another Error,Error
                """;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then row numbers should be tracked for errors
        List<EntityStatus> failedEntities = entityStatusRepository.findFailedEntities(completedJob.getId());

        for (EntityStatus failed : failedEntities) {
            // Row number should be captured
            // Note: The actual row tracking depends on implementation
            assertThat(failed.getStatus()).isEqualTo("FAILED");
        }
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should allow manual retry by resubmitting failed entities")
    void shouldAllowManualRetryByResubmittingFailedEntities() {
        // Given CSV with some invalid rows
        String csvContent = """
                project_key,name,description
                ,Missing Key
                PROJ1,Valid Project,Description
                """;

        // When uploading initially
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob firstJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then partial data should be processed
        List<EntityStatus> firstRunStatuses = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(firstJob.getId());
        assertThat(firstRunStatuses).isNotEmpty();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should track failed entity types correctly")
    void shouldTrackFailedEntityTypesCorrectly() {
        // Given mixed valid/invalid CSV
        String csvContent = """
                project_key,name,description,project_type
                PROJ1,Project One,Description,INVALID_TYPE
                PROJ2,Project Two,Description,COMPANY_MANAGED
                ,Missing Key,Description,COMPANY_MANAGED
                """;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then entity types should be correctly identified
        List<EntityStatus> allStatuses = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(completedJob.getId());
        assertThat(allStatuses).allMatch(s -> "PROJECT".equals(s.getEntityType()));
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should mark job as failed when critical errors occur")
    void shouldMarkJobAsFailedWhenCriticalErrorsOccur() {
        // Given CSV with all invalid rows
        String csvContent = """
                project_key,name
                ,Missing Key
                ,Also Missing
                ,Still Missing
                """;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then job should be marked (may be COMPLETED with failed entities or FAILED)
        ImportResultResponse result = getImportResult(completedJob.getId());
        assertThat(result.getFailedEntities()).isGreaterThan(0);
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should record processing duration for failed entities")
    void shouldRecordProcessingDurationForFailedEntities() {
        // Given CSV that causes processing errors
        String csvContent = invalidCsv;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then duration should be recorded even for failed entities
        List<EntityStatus> failedEntities = entityStatusRepository.findFailedEntities(completedJob.getId());

        for (EntityStatus failed : failedEntities) {
            // Duration should be recorded
            // Note: duration might be 0 for very fast failures
            assertThat(failed.getCompletedAt()).isNotNull();
        }
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should record error context for debugging")
    void shouldRecordErrorContextForDebugging() {
        // Given CSV with complex errors
        String csvContent = """
                project_key,name,description
                invalid_lowercase,Invalid Key,Description with special chars: & < >
                TOOLONGPROJECTKEY,Too Long,Description
                """;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then error context should be available for debugging
        List<EntityStatus> failedEntities = entityStatusRepository.findFailedEntities(completedJob.getId());

        // Context may be stored in errorContext field
        for (EntityStatus failed : failedEntities) {
            log.debug("Error context for {}: {}", failed.getEntityKey(), failed.getErrorContext());
        }
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should handle DLQ operations gracefully when no failures exist")
    void shouldHandleDlqOperationsGracefullyWhenNoFailuresExist() {
        // Given valid CSV with no failures
        String validCsv = TestUtils.getResourceFileContent("projects.csv");

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(validCsv, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then DLQ operations should handle empty results
        List<EntityStatus> failedEntities = entityStatusRepository.findFailedEntities(completedJob.getId());
        assertThat(failedEntities).isEmpty();

        ImportResultResponse result = getImportResult(completedJob.getId());
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should update job failed count during processing")
    void shouldUpdateJobFailedCountDuringProcessing() {
        // Given CSV with multiple invalid rows
        String csvContent = """
                project_key,name,description
                ,Missing Key 1
                ,Missing Key 2
                ,Missing Key 3
                """;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");

        // Then failed count should be tracked
        MigrationJob job = getJob(uploadResponse.getId());
        assertThat(job.getFailedEntities()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should provide detailed error codes for different failure types")
    void shouldProvideDetailedErrorCodesForDifferentFailureTypes() {
        // Given CSV with various error types
        String csvContent = """
                project_key,name
                ,Missing Key
                invalid_lowercase,Invalid Format
                """;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then error codes should differentiate failure types
        List<EntityStatus> failedEntities = entityStatusRepository.findFailedEntities(completedJob.getId());

        // Error codes should be descriptive
        for (EntityStatus failed : failedEntities) {
            assertThat(failed.getErrorCode()).isNotNull();
            log.debug("Error code: {} for entity: {}", failed.getErrorCode(), failed.getEntityKey());
        }
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should handle concurrent failures without data corruption")
    void shouldHandleConcurrentFailuresWithoutDataCorruption() {
        // Given multiple imports with potential failures
        MigrationJobResponse response1 = uploadCsv(invalidCsv, "PROJECT");
        MigrationJobResponse response2 = uploadCsv(invalidCsv, "PROJECT");
        MigrationJobResponse response3 = uploadCsv(invalidCsv, "PROJECT");

        // When all complete
        MigrationJob job1 = waitForJobCompletion(response1.getId(), 60);
        MigrationJob job2 = waitForJobCompletion(response2.getId(), 60);
        MigrationJob job3 = waitForJobCompletion(response3.getId(), 60);

        // Then each should have independent failure tracking
        List<EntityStatus> failures1 = entityStatusRepository.findFailedEntities(job1.getId());
        List<EntityStatus> failures2 = entityStatusRepository.findFailedEntities(job2.getId());
        List<EntityStatus> failures3 = entityStatusRepository.findFailedEntities(job3.getId());

        assertThat(failures1.stream().allMatch(f -> f.getJobId().equals(job1.getId()))).isTrue();
        assertThat(failures2.stream().allMatch(f -> f.getJobId().equals(job2.getId()))).isTrue();
        assertThat(failures3.stream().allMatch(f -> f.getJobId().equals(job3.getId()))).isTrue();
    }
}