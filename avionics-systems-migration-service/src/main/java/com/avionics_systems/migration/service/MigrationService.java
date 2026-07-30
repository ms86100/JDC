package com.avionics_systems.migration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.dto.*;
import com.avionics_systems.migration.entity.*;
import com.avionics_systems.migration.exception.*;
import com.avionics_systems.migration.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationService {

    private final MigrationJobRepository migrationJobRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final UserMappingRepository userMappingRepository;
    private final ProjectMappingRepository projectMappingRepository;
    private final BackupEntityRepository backupEntityRepository;
    private final ObjectMapper objectMapper;
    private final TargetProjectValidator targetProjectValidator;
    private final MigrationAuditPersistenceService migrationAuditPersistenceService;
    private final MigrationJobLogService migrationJobLogService;

    @Transactional
    public MigrationJobResponse startImport(StartMigrationRequest request, UUID userId) {
        log.info("Starting import job: type={}, source={}", request.getJobType(), request.getImportSource());

        if (request.getTargetProjectId() != null) {
            targetProjectValidator.assertProjectExists(request.getTargetProjectId());
        }

        MigrationJob job = MigrationJob.builder()
                .jobType(request.getJobType())
                .jobStatus("PENDING")
                .importSource(request.getImportSource())
                .sourceProjectId(request.getSourceProjectId())
                .targetProjectId(request.getTargetProjectId())
                .config(request.getConfig())
                .options(request.getOptions())
                .initiatedBy(userId)
                .canRollback(true)
                .build();

        job = migrationJobRepository.save(job);
        log.info("Created migration job: id={}", job.getId());
        migrationAuditPersistenceService.log(job.getId(), "JOB_CREATED", "JOB", job.getId().toString(),
                userId, Map.of("importSource", request.getImportSource()));

        return MigrationJobResponse.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public MigrationJobResponse getJobStatus(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        return MigrationJobResponse.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public JobProgressResponse getJobProgress(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        List<EntityStatus> allEntities = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId);

        Map<String, List<EntityStatus>> byType = new HashMap<>();
        for (EntityStatus entity : allEntities) {
            byType.computeIfAbsent(entity.getEntityType(), k -> new ArrayList<>()).add(entity);
        }

        List<JobProgressResponse.EntityTypeProgress> entityProgress = byType.entrySet().stream()
                .map(e -> JobProgressResponse.EntityTypeProgress.builder()
                        .entityType(e.getKey())
                        .total(e.getValue().size())
                        .completed((int) e.getValue().stream().filter(s -> "COMPLETED".equals(s.getStatus())).count())
                        .failed((int) e.getValue().stream().filter(s -> "FAILED".equals(s.getStatus())).count())
                        .pending((int) e.getValue().stream().filter(s -> "PENDING".equals(s.getStatus())).count())
                        .processing((int) e.getValue().stream().filter(s -> "PROCESSING".equals(s.getStatus())).count())
                        .build())
                .toList();

        long elapsedTimeMs = 0;
        if (job.getStartedAt() != null) {
            elapsedTimeMs = java.time.Duration.between(job.getStartedAt(), LocalDateTime.now()).toMillis();
        }

        Long estimatedRemaining = null;
        if (job.getProgressPercentage() > 0 && elapsedTimeMs > 0) {
            double totalEstimated = elapsedTimeMs / (job.getProgressPercentage() / 100.0);
            estimatedRemaining = (long) (totalEstimated - elapsedTimeMs);
        }

        Map<String, JobProgressResponse.StageProgress> stages = extractStages(job.getResultMetadata());

        return JobProgressResponse.builder()
                .jobId(job.getId())
                .jobStatus(job.getJobStatus())
                .errorMessage(job.getErrorMessage())
                .progressPercentage(job.getProgressPercentage())
                .totalEntities(job.getTotalEntities())
                .processedEntities(job.getProcessedEntities())
                .failedEntities(job.getFailedEntities())
                .completedEntities((int) allEntities.stream().filter(s -> "COMPLETED".equals(s.getStatus())).count())
                .pendingEntities((int) allEntities.stream().filter(s -> "PENDING".equals(s.getStatus())).count())
                .processingEntities((int) allEntities.stream().filter(s -> "PROCESSING".equals(s.getStatus())).count())
                .skippedEntities((int) allEntities.stream().filter(s -> "SKIPPED".equals(s.getStatus())).count())
                .elapsedTimeMs(elapsedTimeMs)
                .estimatedTimeRemainingMs(estimatedRemaining)
                .entityProgress(entityProgress)
                .stages(stages)
                .currentPhase(metadataPhase(job.getResultMetadata()))
                .recentLogs(migrationJobLogService.getRecentLogs(jobId))
                .attachmentBytesWritten(metadataLong(job.getResultMetadata(), "attachmentBytesWritten"))
                .attachmentsCompleted(metadataInt(job.getResultMetadata(), "attachmentsCompleted"))
                .incrementalSkipped(metadataInt(job.getResultMetadata(), "incrementalSkipped"))
                .attachmentChunkIndex(metadataInt(job.getResultMetadata(), "attachmentChunkIndex"))
                .attachmentChunkTotal(metadataInt(job.getResultMetadata(), "attachmentChunkTotal"))
                .attachmentCurrentFile(metadataString(job.getResultMetadata(), "attachmentCurrentFile"))
                .attachmentChunked(metadataBoolean(job.getResultMetadata(), "attachmentChunked"))
                .build();
    }

    private static String metadataPhase(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object phase = metadata.get("currentPhase");
        return phase != null ? phase.toString() : null;
    }

    private static Long metadataLong(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object v = metadata.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private static Integer metadataInt(Map<String, Object> metadata, String key) {
        Long l = metadataLong(metadata, key);
        return l != null ? l.intValue() : null;
    }

    private static String metadataString(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object v = metadata.get(key);
        return v != null ? v.toString() : null;
    }

    private static Boolean metadataBoolean(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object v = metadata.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(v.toString());
    }

    @Transactional(readOnly = true)
    public ImportResultResponse getImportResult(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        List<EntityStatus> failed = entityStatusRepository.findFailedEntities(jobId);
        List<EntityStatus> all = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId);

        List<ImportResultResponse.EntityError> errors = failed.stream()
                .map(e -> ImportResultResponse.EntityError.builder()
                        .entityType(e.getEntityType())
                        .entityKey(e.getEntityKey())
                        .row(e.getErrorRow())
                        .field(e.getErrorField())
                        .errorCode(e.getErrorCode())
                        .errorMessage(e.getErrorMessage())
                        .build())
                .toList();

        List<ImportResultResponse.EntityWarning> warnings = all.stream()
                .filter(e -> e.getWarnings() != null)
                .map(e -> {
                    // Parse warnings JSON
                    List<ImportResultResponse.EntityWarning> warningList = new ArrayList<>();
                    try {
                        if (e.getWarnings() != null) {
                            List<String> parsed = objectMapper.readValue(e.getWarnings(), new TypeReference<>() {});
                            for (String w : parsed) {
                                warningList.add(ImportResultResponse.EntityWarning.builder()
                                        .entityType(e.getEntityType())
                                        .entityKey(e.getEntityKey())
                                        .warningMessage(w)
                                        .build());
                            }
                        }
                    } catch (JsonProcessingException ex) {
                        log.warn("Failed to parse warnings for entity {}", e.getEntityKey(), ex);
                    }
                    return warningList;
                })
                .flatMap(List::stream)
                .toList();

        Long durationMs = null;
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            durationMs = java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis();
        }

        return ImportResultResponse.builder()
                .jobId(job.getId())
                .jobStatus(job.getJobStatus())
                .totalEntities(job.getTotalEntities())
                .processedEntities(job.getProcessedEntities())
                .failedEntities(job.getFailedEntities())
                .successCount(Math.max(0, job.getProcessedEntities() != null ? job.getProcessedEntities() : 0))
                .warningCount(warnings.size())
                .errors(errors)
                .warnings(warnings)
                .resultMetadata(job.getResultMetadata())
                .durationMs(durationMs)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, JobProgressResponse.StageProgress> extractStages(Map<String, Object> metadata) {
        if (metadata == null || !(metadata.get("stages") instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, JobProgressResponse.StageProgress> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            if (e.getValue() instanceof Map<?, ?> stageMap) {
                Object completed = stageMap.get("completed");
                Object total = stageMap.get("total");
                out.put(String.valueOf(e.getKey()), JobProgressResponse.StageProgress.builder()
                        .completed(completed instanceof Number n ? n.intValue() : 0)
                        .total(total instanceof Number n ? n.intValue() : 0)
                        .build());
            }
        }
        return out;
    }

    @Transactional
    public void cancelJob(UUID jobId, UUID userId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        if (!"PENDING".equals(job.getJobStatus()) && !"IN_PROGRESS".equals(job.getJobStatus())) {
            throw new MigrationException("Cannot cancel job in status: " + job.getJobStatus());
        }

        job.setJobStatus("CANCELLED");
        job.setCompletedAt(LocalDateTime.now());
        migrationJobRepository.save(job);
    }

    @Transactional
    public void markJobStarted(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        job.markStarted();
        migrationJobRepository.save(job);
    }

    @Transactional
    public void markJobCompleted(UUID jobId, Map<String, Object> resultMetadata) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        job.markCompleted();
        job.setResultMetadata(resultMetadata);
        migrationJobRepository.save(job);
    }

    @Transactional
    public void markJobFailed(UUID jobId, String errorMessage, Map<String, Object> errorDetails) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        job.markFailed(errorMessage);
        job.setErrorDetails(errorDetails);
        migrationJobRepository.save(job);
    }

    @Transactional
    public void updateJobProgress(UUID jobId, int processed, int failed) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        job.setProcessedEntities(processed);
        job.setFailedEntities(failed);
        if (job.getTotalEntities() > 0) {
            job.setProgressPercentage((processed + failed) / (double) job.getTotalEntities() * 100.0);
        }
        migrationJobRepository.save(job);
    }

    @Transactional
    public void setTotalEntities(UUID jobId, int total) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        job.setTotalEntities(total);
        migrationJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Page<MigrationJobResponse> listJobs(String status, String type, UUID userId, Pageable pageable) {
        log.debug("Listing jobs: status={}, type={}, userId={}, page={}",
                status, type, userId, pageable.getPageNumber());

        Page<MigrationJob> jobs;

        if (status != null && userId != null) {
            jobs = migrationJobRepository.findByUserAndStatus(userId, status, pageable);
        } else if (status != null) {
            jobs = migrationJobRepository.findByJobStatus(status, pageable);
        } else if (type != null) {
            jobs = migrationJobRepository.findByJobType(type, pageable);
        } else if (userId != null) {
            jobs = migrationJobRepository.findByInitiatedBy(userId, pageable);
        } else {
            jobs = migrationJobRepository.findAll(pageable);
        }

        return jobs.map(MigrationJobResponse::fromEntity);
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object", e);
            return null;
        }
    }
}