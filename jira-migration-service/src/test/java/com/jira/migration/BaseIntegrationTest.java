package com.jira.migration;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.repository.ProjectMappingRepository;
import com.jira.migration.service.MigrationService;
import com.jira.migration.dto.StartMigrationRequest;
import com.jira.migration.dto.MigrationJobResponse;
import com.jira.migration.dto.ImportResultResponse;
import com.jira.migration.dto.JobProgressResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration tests providing common utilities and test setup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
@Component
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected MigrationJobRepository jobRepository;

    @Autowired
    protected EntityStatusRepository entityStatusRepository;

    @Autowired
    protected ProjectMappingRepository projectMappingRepository;

    @Autowired
    protected MigrationService migrationService;

    protected final UUID testUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @AfterEach
    void cleanupTestData() {
        // Clean up entity statuses first (foreign key dependency)
        entityStatusRepository.deleteAll();
        // Clean up project mappings
        projectMappingRepository.deleteAll();
        // Clean up migration jobs
        jobRepository.deleteAll();
    }

    /**
     * Uploads CSV content via the REST API and returns the file ID.
     *
     * @param csvContent the CSV content to upload
     * @return the MigrationJobResponse from the upload
     */
    protected MigrationJobResponse uploadCsv(String csvContent, String entityType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", testUserId.toString());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-import.csv";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = "http://localhost:" + port + "/api/migration/import/csv";
        ResponseEntity<MigrationJobResponse> response = restTemplate.postForEntity(
                url,
                requestEntity,
                MigrationJobResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("CSV upload should return 202 Accepted")
                .isTrue();
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    /**
     * Uploads CSV content for a specific template.
     *
     * @param csvContent the CSV content to upload
     * @param templateId the template UUID
     * @return the MigrationJobResponse from the upload
     */
    protected MigrationJobResponse uploadCsvWithTemplate(String csvContent, UUID templateId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", testUserId.toString());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test-import.csv";
            }
        });
        body.add("templateId", templateId.toString());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = "http://localhost:" + port + "/api/migration/import/csv";
        ResponseEntity<MigrationJobResponse> response = restTemplate.postForEntity(
                url,
                requestEntity,
                MigrationJobResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    /**
     * Waits for a migration job to complete or fail.
     *
     * @param jobId the UUID of the job to wait for
     * @param timeoutSeconds maximum time to wait
     * @return the completed MigrationJob
     */
    protected MigrationJob waitForJobCompletion(UUID jobId, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeout = TimeUnit.SECONDS.toMillis(timeoutSeconds);

        while (System.currentTimeMillis() - startTime < timeout) {
            MigrationJobResponse status = getJobStatus(jobId);
            String jobStatus = status.getJobStatus();

            log.debug("Job {} status: {}", jobId, jobStatus);

            if ("COMPLETED".equals(jobStatus) || "FAILED".equals(jobStatus) || "CANCELLED".equals(jobStatus)) {
                return jobRepository.findById(jobId).orElseThrow();
            }

            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new RuntimeException("Job " + jobId + " did not complete within " + timeoutSeconds + " seconds");
    }

    /**
     * Gets the current status of a job via REST API.
     *
     * @param jobId the UUID of the job
     * @return the MigrationJobResponse
     */
    protected MigrationJobResponse getJobStatus(UUID jobId) {
        String url = "http://localhost:" + port + "/api/migration/jobs/" + jobId;
        ResponseEntity<MigrationJobResponse> response = restTemplate.getForEntity(
                url,
                MigrationJobResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    /**
     * Gets the progress of a job via REST API.
     *
     * @param jobId the UUID of the job
     * @return the JobProgressResponse
     */
    protected JobProgressResponse getJobProgress(UUID jobId) {
        String url = "http://localhost:" + port + "/api/migration/jobs/" + jobId + "/progress";
        ResponseEntity<JobProgressResponse> response = restTemplate.getForEntity(
                url,
                JobProgressResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    /**
     * Gets the import result of a job via REST API.
     *
     * @param jobId the UUID of the job
     * @return the ImportResultResponse
     */
    protected ImportResultResponse getImportResult(UUID jobId) {
        String url = "http://localhost:" + port + "/api/migration/jobs/" + jobId + "/result";
        ResponseEntity<ImportResultResponse> response = restTemplate.getForEntity(
                url,
                ImportResultResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    /**
     * Cancels a running job.
     *
     * @param jobId the UUID of the job to cancel
     */
    protected void cancelJob(UUID jobId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", testUserId.toString());

        String url = "http://localhost:" + port + "/api/migration/jobs/" + jobId + "/cancel";
        ResponseEntity<Void> response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(headers),
                Void.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 304)
                .as("Job cancellation should return success")
                .isTrue();
    }

    /**
     * Gets a job from the repository.
     *
     * @param jobId the UUID of the job
     * @return the MigrationJob entity
     */
    protected MigrationJob getJob(UUID jobId) {
        return jobRepository.findById(jobId).orElseThrow();
    }

    /**
     * Gets all entity statuses for a job.
     *
     * @param jobId the UUID of the job
     * @return list of EntityStatus entities
     */
    protected List<EntityStatus> getEntityStatuses(UUID jobId) {
        return entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId);
    }

    /**
     * Gets all project mappings for a job.
     *
     * @param jobId the UUID of the job
     * @return list of ProjectMapping entities
     */
    protected List<ProjectMapping> getProjectMappings(UUID jobId) {
        return projectMappingRepository.findByJobId(jobId);
    }

    /**
     * Creates a test migration job directly via the service.
     *
     * @param jobType the job type (IMPORT, EXPORT)
     * @param importSource the import source (CSV, JIRA_DC, etc.)
     * @return the created MigrationJob
     */
    protected MigrationJob createTestJob(String jobType, String importSource) {
        StartMigrationRequest request = StartMigrationRequest.builder()
                .jobType(jobType)
                .importSource(importSource)
                .build();

        MigrationJobResponse response = migrationService.startImport(request, testUserId);
        return jobRepository.findById(response.getId()).orElseThrow();
    }
}
