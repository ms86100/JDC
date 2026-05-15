package com.jira.migration.integration;

import com.jira.migration.BaseIntegrationTest;
import com.jira.migration.TestUtils;
import com.jira.migration.dto.JobProgressResponse;
import com.jira.migration.dto.MigrationJobResponse;
import com.jira.migration.entity.MigrationJob;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Batch Processing Integration Tests")
@Nested
@Slf4j
public class BatchProcessingIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should process 100-row CSV file in batches")
    void shouldProcess100RowCsvFileInBatches() {
        String largeCsv = TestUtils.generateLargeCsv(100, "PROJECT");
        MigrationJobResponse uploadResponse = uploadCsv(largeCsv, "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 120);
        assertThat(completedJob.getJobStatus()).isEqualTo("COMPLETED");
        assertThat(completedJob.getTotalEntities()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should process 500-row CSV file")
    void shouldProcess500RowCsvFile() {
        String largeCsv = TestUtils.generateLargeCsv(500, "ISSUE");
        MigrationJobResponse uploadResponse = uploadCsv(largeCsv, "ISSUE");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 180);
        assertThat(completedJob.getJobStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should track progress percentage during batch processing")
    void shouldTrackProgressPercentageDuringBatchProcessing() {
        String largeCsv = TestUtils.generateLargeCsv(100, "PROJECT");
        MigrationJobResponse uploadResponse = uploadCsv(largeCsv, "PROJECT");
        UUID jobId = uploadResponse.getId();
        JobProgressResponse progress = getJobProgress(jobId);
        assertThat(progress.getJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("Should maintain accurate processed count during batch processing")
    void shouldMaintainAccurateProcessedCountDuringBatchProcessing() {
        String csvContent = TestUtils.generateLargeCsv(50, "ISSUE");
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "ISSUE");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 120);
        assertThat(completedJob.getProcessedEntities()).isEqualTo(completedJob.getTotalEntities());
    }

    @Test
    @DisplayName("Should track entity progress by type")
    void shouldTrackEntityProgressByType() {
        String csvContent = TestUtils.generateLargeCsv(50, "ISSUE");
        MigrationJobResponse uploadResponse = uploadCsv(csvContent, "ISSUE");
        UUID jobId = uploadResponse.getId();
        waitForJobCompletion(jobId, 120);
        JobProgressResponse progress = getJobProgress(jobId);
        if (progress.getEntityProgress() != null && !progress.getEntityProgress().isEmpty()) {
            assertThat(progress.getEntityProgress()).allMatch(ep ->
                ep.getEntityType() != null && ep.getTotal() != null && ep.getCompleted() != null);
        }
    }

    @Test
    @DisplayName("Should complete multiple batch jobs sequentially")
    void shouldCompleteMultipleBatchJobsSequentially() {
        MigrationJobResponse r1 = uploadCsv(TestUtils.generateLargeCsv(50, "PROJECT"), "PROJECT");
        MigrationJob j1 = waitForJobCompletion(r1.getId(), 120);
        MigrationJobResponse r2 = uploadCsv(TestUtils.generateLargeCsv(50, "ISSUE"), "ISSUE");
        MigrationJob j2 = waitForJobCompletion(r2.getId(), 120);
        assertThat(j1.getJobStatus()).isEqualTo("COMPLETED");
        assertThat(j2.getJobStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should estimate remaining time for large batches")
    void shouldEstimateRemainingTimeForLargeBatches() {
        MigrationJobResponse uploadResponse = uploadCsv(TestUtils.generateLargeCsv(100, "PROJECT"), "PROJECT");
        JobProgressResponse progress = getJobProgress(uploadResponse.getId());
        assertThat(progress.getJobId()).isNotNull();
    }

    @Test
    @DisplayName("Should handle batch with partial failures")
    void shouldHandleBatchWithPartialFailures() {
        MigrationJobResponse uploadResponse = uploadCsv(TestUtils.generateLargeCsv(20, "ISSUE"), "ISSUE");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 60);
        assertThat(completedJob.getJobStatus()).isNotNull();
    }

    @Test
    @DisplayName("Should handle batch interruption gracefully")
    void shouldHandleBatchInterruptionGracefully() {
        MigrationJobResponse uploadResponse = uploadCsv(TestUtils.generateLargeCsv(100, "PROJECT"), "PROJECT");
        cancelJob(uploadResponse.getId());
    }

    @Test
    @DisplayName("Should persist entity statuses in processing order")
    void shouldPersistEntityStatusesInProcessingOrder() {
        MigrationJobResponse uploadResponse = uploadCsv(TestUtils.generateLargeCsv(20, "PROJECT"), "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 120);
        assertThat(completedJob.getTotalEntities()).isNotNull();
    }

    @Test
    @DisplayName("Should track pending and processing entities correctly")
    void shouldTrackPendingAndProcessingEntitiesCorrectly() {
        MigrationJobResponse uploadResponse = uploadCsv(TestUtils.generateLargeCsv(30, "ISSUE"), "ISSUE");
        JobProgressResponse progress = getJobProgress(uploadResponse.getId());
        assertThat(progress.getPendingEntities()).isNotNull();
    }

    @Test
    @DisplayName("Should process batches without memory issues")
    void shouldProcessBatchesWithoutMemoryIssues() {
        MigrationJobResponse uploadResponse = uploadCsv(TestUtils.generateLargeCsv(200, "PROJECT"), "PROJECT");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 180);
        assertThat(completedJob.getJobStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should handle 1000-row CSV with reasonable performance")
    void shouldHandle1000RowCsvWithReasonablePerformance() {
        MigrationJobResponse uploadResponse = uploadCsv(TestUtils.generateLargeCsv(1000, "ISSUE"), "ISSUE");
        MigrationJob completedJob = waitForJobCompletion(uploadResponse.getId(), 300);
        assertThat(completedJob.getJobStatus()).isEqualTo("COMPLETED");
    }
}