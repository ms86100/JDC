package com.jira.migration.service;

import com.jira.migration.entity.MigrationJob;
import com.jira.migration.exception.EntityNotFoundException;
import com.jira.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MigrationJobControlService {

    private final MigrationJobRepository jobRepository;
    private final MigrationJobLogService jobLogService;

    @Transactional
    public Map<String, Object> pauseJob(UUID jobId) {
        MigrationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        if (!"IN_PROGRESS".equals(job.getJobStatus())) {
            throw new IllegalStateException("Only IN_PROGRESS jobs can be paused");
        }
        job.setJobStatus("PAUSED");
        Map<String, Object> meta = job.getResultMetadata() != null
                ? new HashMap<>(job.getResultMetadata()) : new HashMap<>();
        meta.put("paused", true);
        meta.put("pausedAt", java.time.Instant.now().toString());
        job.setResultMetadata(meta);
        jobRepository.save(job);
        jobLogService.appendLog(jobId, "INFO", "Job paused by user");
        return Map.of("jobId", jobId.toString(), "status", "PAUSED");
    }

    @Transactional
    public Map<String, Object> resumeJob(UUID jobId) {
        MigrationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
        if (!"PAUSED".equals(job.getJobStatus())) {
            throw new IllegalStateException("Job is not paused");
        }
        job.setJobStatus("IN_PROGRESS");
        Map<String, Object> meta = job.getResultMetadata() != null
                ? new HashMap<>(job.getResultMetadata()) : new HashMap<>();
        meta.put("paused", false);
        meta.put("resumedAt", java.time.Instant.now().toString());
        job.setResultMetadata(meta);
        jobRepository.save(job);
        jobLogService.appendLog(jobId, "INFO", "Job resumed by user");
        return Map.of("jobId", jobId.toString(), "status", "IN_PROGRESS");
    }

    @Transactional(readOnly = true)
    public boolean isPaused(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(j -> "PAUSED".equals(j.getJobStatus()))
                .orElse(false);
    }
}
