package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.entity.WizardSession;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import com.avionics_systems.migration.repository.WizardSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkflowStatusMappingService {

    private final MigrationJobRepository migrationJobRepository;
    private final WizardSessionRepository wizardSessionRepository;

    @Transactional
    public Map<String, Object> saveForJob(UUID jobId, Map<String, Object> mappings) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        Map<String, Object> merged = job.getOptions() != null ? new HashMap<>(job.getOptions()) : new HashMap<>();
        merged.put("workflowStatusMappings", mappings);
        job.setWorkflowStatusMappings(mappings);
        job.setOptions(merged);
        migrationJobRepository.save(job);
        return mappings;
    }

    @Transactional
    public Map<String, Object> saveForSession(UUID sessionId, Map<String, Object> mappings) {
        WizardSession session = wizardSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        session.setWorkflowStatusMappings(mappings);
        wizardSessionRepository.save(session);
        return mappings;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getForJob(UUID jobId) {
        return migrationJobRepository.findById(jobId)
                .map(MigrationJob::getWorkflowStatusMappings)
                .orElse(Map.of());
    }

    public String resolveStatus(Map<String, Object> mappings, String sourceStatus) {
        if (sourceStatus == null || mappings == null) {
            return sourceStatus;
        }
        Object statusMap = mappings.get("status");
        if (statusMap instanceof Map<?, ?> map) {
            Object target = map.get(sourceStatus);
            if (target != null) {
                return target.toString();
            }
        }
        return sourceStatus;
    }
}
