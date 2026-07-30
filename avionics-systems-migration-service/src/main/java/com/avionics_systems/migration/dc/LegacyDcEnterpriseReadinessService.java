package com.avionics_systems.migration.dc;

import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.exception.EntityNotFoundException;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LegacyDcEnterpriseReadinessService {

    private final MigrationJobRepository migrationJobRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSlaProof(UUID jobId) {
        MigrationJob job = loadJob(jobId);
        Map<String, Object> meta = job.getResultMetadata();
        if (meta != null && meta.get("slaProof") instanceof Map<?, ?> existing) {
            @SuppressWarnings("unchecked")
            Map<String, Object> copy = (Map<String, Object>) existing;
            return copy;
        }
        return buildSlaProofFromJob(job);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAcSignoff(UUID jobId) {
        MigrationJob job = loadJob(jobId);
        Map<String, Object> meta = job.getResultMetadata();
        if (meta != null && meta.get("acSignoff") instanceof Map<?, ?> existing) {
            @SuppressWarnings("unchecked")
            Map<String, Object> copy = (Map<String, Object>) existing;
            return copy;
        }
        return LegacyDcAcSignoffEvaluator.evaluate(
                meta,
                parseOptions(job),
                job.getJobStatus() != null ? job.getJobStatus() : "UNKNOWN",
                job.getTotalEntities() != null ? job.getTotalEntities() : 0,
                job.getFailedEntities() != null ? job.getFailedEntities() : 0);
    }

    public Map<String, Object> buildSlaProofFromJob(MigrationJob job) {
        Map<String, Object> meta = job.getResultMetadata() != null ? job.getResultMetadata() : Map.of();
        int issueCount = countIssues(meta);
        long durationMs = jobDurationMs(job);
        int failed = job.getFailedEntities() != null ? job.getFailedEntities() : 0;
        boolean stub = Boolean.TRUE.equals(meta.get("stubDownstream"));
        return LegacyDcImportSlaProofBuilder.build(issueCount, durationMs, failed, stub, "LIVE_IMPORT_JOB");
    }

    private MigrationJob loadJob(UUID jobId) {
        return migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));
    }

    private static int countIssues(Map<String, Object> meta) {
        if (meta.get("processedByType") instanceof Map<?, ?> byType) {
            int issues = 0;
            for (Map.Entry<?, ?> e : byType.entrySet()) {
                if ("Issue".equals(String.valueOf(e.getKey())) || "SubTask".equals(String.valueOf(e.getKey()))) {
                    issues += e.getValue() instanceof Number n ? n.intValue() : 0;
                }
            }
            if (issues > 0) {
                return issues;
            }
        }
        if (meta.get("entitiesExpected") instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static long jobDurationMs(MigrationJob job) {
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            return Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis();
        }
        if (job.getResultMetadata() != null && job.getResultMetadata().get("durationMs") instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private Map<String, Object> parseOptions(MigrationJob job) {
        return job.getOptions() != null ? job.getOptions() : Map.of();
    }
}
