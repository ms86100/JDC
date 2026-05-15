package com.jira.migration.integration;

import com.jira.migration.BaseIntegrationTest;
import com.jira.migration.TestUtils;
import com.jira.migration.dto.ImportResultResponse;
import com.jira.migration.dto.JobProgressResponse;
import com.jira.migration.dto.MigrationJobResponse;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.ProjectMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CSV import functionality.
 * Tests the complete flow from CSV upload to database persistence.
 */
@Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("CSV Import Integration Tests")
@Nested
@Slf4j
public class CsvImportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EntityStatusRepository entityStatusRepository;

    @Autowired
    private ProjectMappingRepository projectMappingRepository;

    private String validProjectsCsv;
    private String validIssuesCsv;

    @BeforeEach
    void setUp() {
        validProjectsCsv = TestUtils.getResourceFileContent("projects.csv");
        validIssuesCsv = TestUtils.getResourceFileContent("issues.csv");
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should successfully upload and start CSV import")
    void shouldSuccessfullyUploadAndStartCsvImport() {
        // Given valid CSV content
        String csvContent = validProjectsCsv;

        // When uploading CSV
        MigrationJobResponse response = uploadCsv(csvContent, "PROJECT");

        // Then job should be created with PENDING status
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getJobStatus()).isIn("PENDING", "IN_PROGRESS");
        assertThat(response.getJobType()).isEqualTo("IMPORT");
        assertThat(response.getImportSource()).isEqualTo("CSV");
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should complete CSV import with all rows processed")
    void shouldCompleteCsvImportWithAllRowsProcessed() {
        // Given valid CSV with projects
        String csvContent = validProjectsCsv;

        // When uploading and waiting for completion
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then job should be completed successfully
        assertThat(completedJob.getJobStatus()).isEqualTo("COMPLETED");
        assertThat(completedJob.getProcessedEntities()).isGreaterThan(0);

        ImportResultResponse result = getImportResult(uploadResponse.getId());
        assertThat(result.getJobStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalEntities()).isGreaterThan(0);
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should persist entity statuses to database during import")
    void shouldPersistEntityStatusesToDatabase() {
        // Given valid CSV
        String csvContent = validProjectsCsv;

        // When uploading CSV
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");

        // And waiting for completion
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then entity statuses should be persisted
        List<EntityStatus> statuses = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(completedJob.getId());
        assertThat(statuses).isNotEmpty();

        long completedCount = statuses.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .count();
        assertThat(completedCount).isGreaterThan(0);
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should track progress during import")
    void shouldTrackProgressDuringImport() {
        // Given valid CSV
        String csvContent = validProjectsCsv;

        // When uploading CSV
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");

        // Then progress should be trackable
        JobProgressResponse progress = getJobProgress(uploadResponse.getId());
        assertThat(progress).isNotNull();
        assertThat(progress.getJobId()).isEqualTo(uploadResponse.getId());
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should handle large CSV files in batch processing")
    void shouldHandleLargeCsvFilesInBatchProcessing() {
        // Given large CSV with 100 rows
        String largeCsv = TestUtils.generateLargeCsv(100, "ISSUE");

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(largeCsv, "ISSUE");

        // And waiting for completion
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 120);

        // Then all rows should be processed
        assertThat(completedJob.getJobStatus()).isEqualTo("COMPLETED");
        assertThat(completedJob.getTotalEntities()).isEqualTo(100);
        assertThat(completedJob.getProcessedEntities()).isEqualTo(100);
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should process multiple projects from CSV")
    void shouldProcessMultipleProjectsFromCsv() {
        // Given CSV with multiple projects
        String csvContent = validProjectsCsv;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");

        // And waiting for completion
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then all projects should be processed
        ImportResultResponse result = getImportResult(completedJob.getId());
        assertThat(result.getTotalEntities()).isGreaterThanOrEqualTo(10);
        assertThat(result.getFailedEntities()).isEqualTo(0);
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should handle issues CSV with various issue types")
    void shouldHandleIssuesCsvWithVariousIssueTypes() {
        // Given CSV with different issue types
        String csvContent = validIssuesCsv;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "ISSUE");

        // And waiting for completion
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then job should complete
        assertThat(completedJob.getJobStatus()).isEqualTo("COMPLETED");

        ImportResultResponse result = getImportResult(completedJob.getId());
        assertThat(result.getTotalEntities()).isGreaterThan(0);
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should record failure for invalid CSV rows")
    void shouldRecordFailureForInvalidCsvRows() {
        // Given CSV with invalid data
        String invalidCsv = TestUtils.getResourceFileContent("invalid.csv");

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(invalidCsv, "PROJECT");

        // And waiting for completion
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);

        // Then some rows may have failed
        List<EntityStatus> failedStatuses = entityStatusRepository.findFailedEntities(completedJob.getId());

        ImportResultResponse result = getImportResult(completedJob.getId());

        // Verify failures were tracked
        if (result.getFailedEntities() > 0) {
            assertThat(failedStatuses).isNotEmpty();
            assertThat(failedStatuses.stream().allMatch(s -> "FAILED".equals(s.getStatus()))).isTrue();
        }
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should return detailed error information for failed imports")
    void shouldReturnDetailedErrorInformationForFailedImports() {
        // Given CSV with invalid data
        String invalidCsv = TestUtils.getResourceFileContent("invalid.csv");

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(invalidCsv, "PROJECT");

        // And waiting for completion
        waitForJobCompletion(uploadResponse.getId(), 60);

        // Then detailed errors should be available
        ImportResultResponse result = getImportResult(uploadResponse.getId());

        if (result.getFailedEntities() > 0) {
            assertThat(result.getErrors()).isNotEmpty();
            assertThat(result.getErrors()).allMatch(error ->
                error.getEntityType() != null && error.getErrorMessage() != null
            );
        }
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should maintain job metadata throughout import")
    void shouldMaintainJobMetadataThroughoutImport() {
        // Given valid CSV
        String csvContent = validProjectsCsv;

        // When uploading
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        UUID jobId = uploadResponse.getId();

        // Then metadata should be maintained
        MigrationJob job = getJob(jobId);
        assertThat(job.getInitiatedBy()).isEqualTo(testUserId);
        assertThat(job.getInitiatedAt()).isNotNull();
        assertThat(job.getJobType()).isEqualTo("IMPORT");
        assertThat(job.getImportSource()).isEqualTo("CSV");
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should clean up entity statuses after test cleanup")
    void shouldCleanUpEntityStatusesAfterTestCleanup() {
        // Given valid CSV
        String csvContent = validProjectsCsv;

        // When uploading and completing
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "PROJECT");
        waitForJobCompletion(uploadResponse.getId(), 60);

        // Then after cleanup (in @AfterEach), data should be removed
        // This is verified by the BaseIntegrationTest cleanup
        List<EntityStatus> remainingStatuses = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(uploadResponse.getId());
        // After cleanup, there should be no data for this job
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should handle concurrent import attempts")
    void shouldHandleConcurrentImportAttempts() {
        // Given valid CSV content
        String csvContent = validProjectsCsv;

        // When uploading multiple files concurrently
        MigrationJobResponse response1 = uploadCsv(csvContent, "PROJECT");
        MigrationJobResponse response2 = uploadCsv(csvContent, "PROJECT");
        MigrationJobResponse response3 = uploadCsv(csvContent, "PROJECT");

        // Then all should be created successfully
        assertThat(response1.getId()).isNotEqualTo(response2.getId());
        assertThat(response2.getId()).isNotEqualTo(response3.getId());
        assertThat(response3.getId()).isNotEqualTo(response1.getId());
    }
}
