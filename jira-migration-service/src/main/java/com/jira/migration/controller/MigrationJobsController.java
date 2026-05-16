package com.jira.migration.controller;

import com.jira.migration.batch.DeadLetterQueueService;
import com.jira.migration.dto.MigrationJobResponse;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.service.MigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Migration Job Management.
 * Provides CRUD operations for migration jobs at /api/migration/jobs
 */
@RestController
@RequestMapping("/api/migration/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Migration Jobs", description = "Migration job CRUD operations")
public class MigrationJobsController {

    private final MigrationService migrationService;
    private final MigrationJobRepository migrationJobRepository;

    /**
     * GET /api/migration/jobs - List all migration jobs
     */
    @GetMapping
    @Operation(summary = "List migration jobs", description = "Returns paginated list of migration jobs")
    public ResponseEntity<Page<MigrationJobResponse>> listJobs(
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by type") @RequestParam(required = false) String type,
            @Parameter(description = "Filter by user") @RequestParam(required = false) UUID userId,
            Pageable pageable) {

        log.info("Listing migration jobs: status={}, type={}, userId={}", status, type, userId);
        Page<MigrationJobResponse> jobs = migrationService.listJobs(status, type, userId, pageable);
        return ResponseEntity.ok(jobs);
    }

    /**
     * POST /api/migration/jobs - Create a new migration job
     */
    @PostMapping
    @Operation(summary = "Create migration job", description = "Creates a new migration job")
    public ResponseEntity<MigrationJobResponse> createJob(
            @RequestBody CreateMigrationJobRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Creating migration job: type={}, source={}", request.getJobType(), request.getImportSource());

        MigrationJob job = MigrationJob.builder()
                .jobType(request.getJobType())
                .jobStatus("PENDING")
                .importSource(request.getImportSource())
                .sourceProjectId(request.getSourceProjectId())
                .targetProjectId(request.getTargetProjectId())
                .initiatedBy(userId)
                .canRollback(true)
                .build();

        MigrationJob saved = migrationJobRepository.save(job);
        log.info("Created migration job: id={}", saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(MigrationJobResponse.fromEntity(saved));
    }

    /**
     * GET /api/migration/jobs/{id} - Get job status
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get job status", description = "Returns status of a specific migration job")
    public ResponseEntity<MigrationJobResponse> getJobStatus(
            @Parameter(description = "Job ID") @PathVariable UUID id) {

        log.info("Getting job status: id={}", id);
        return migrationService.getJobStatus(id) != null ?
                ResponseEntity.ok(migrationService.getJobStatus(id)) :
                ResponseEntity.notFound().build();
    }

    /**
     * PUT /api/migration/jobs/{id} - Update job
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update migration job", description = "Updates an existing migration job")
    public ResponseEntity<MigrationJobResponse> updateJob(
            @Parameter(description = "Job ID") @PathVariable UUID id,
            @RequestBody UpdateMigrationJobRequest request) {

        log.info("Updating migration job: id={}", id);

        return migrationJobRepository.findById(id)
                .map(job -> {
                    if (request.getJobStatus() != null) {
                        job.setJobStatus(request.getJobStatus());
                    }
                    if (request.getOptions() != null) {
                        job.setOptions(request.getOptions());
                    }
                    if (request.getFilePath() != null) {
                        job.setFilePath(request.getFilePath());
                    }
                    if (request.getCanRollback() != null) {
                        job.setCanRollback(request.getCanRollback());
                    }
                    MigrationJob saved = migrationJobRepository.save(job);
                    return ResponseEntity.ok(MigrationJobResponse.fromEntity(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/migration/jobs/{id} - Delete job
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete migration job", description = "Deletes a migration job")
    public ResponseEntity<Void> deleteJob(
            @Parameter(description = "Job ID") @PathVariable UUID id) {

        log.info("Deleting migration job: id={}", id);

        if (migrationJobRepository.existsById(id)) {
            migrationJobRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Request DTOs

    @lombok.Data
    public static class CreateMigrationJobRequest {
        private String jobType;
        private String importSource;
        private UUID sourceProjectId;
        private UUID targetProjectId;
        private UUID templateId;
    }

    @lombok.Data
    public static class UpdateMigrationJobRequest {
        private String jobStatus;
        private String options;
        private String filePath;
        private Boolean canRollback;
    }
}