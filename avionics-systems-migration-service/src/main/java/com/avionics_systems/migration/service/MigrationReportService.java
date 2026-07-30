package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.EntityStatus;
import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.exception.EntityNotFoundException;
import com.avionics_systems.migration.entity.MigrationAttachmentResult;
import com.avionics_systems.migration.entity.MigrationIssueResult;
import com.avionics_systems.migration.repository.EntityStatusRepository;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MigrationReportService {

    private final MigrationJobRepository migrationJobRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final MigrationIssueResultService issueResultService;
    private final MigrationAttachmentResultService attachmentResultService;

    @Transactional(readOnly = true)
    public String buildImportReportCsv(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        List<EntityStatus> entities = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(jobId);

        StringBuilder csv = new StringBuilder();
        csv.append("job_id,job_status,import_source,total_entities,processed_entities,failed_entities,initiated_at\n");
        csv.append(job.getId()).append(',')
                .append(escape(job.getJobStatus())).append(',')
                .append(escape(job.getImportSource())).append(',')
                .append(job.getTotalEntities()).append(',')
                .append(job.getProcessedEntities()).append(',')
                .append(job.getFailedEntities()).append(',')
                .append(job.getInitiatedAt() != null
                        ? job.getInitiatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                .append('\n');
        csv.append('\n');
        csv.append("entity_type,entity_key,status,error_code,error_message,error_row\n");

        for (EntityStatus e : entities) {
            csv.append(escape(e.getEntityType())).append(',')
                    .append(escape(e.getEntityKey())).append(',')
                    .append(escape(e.getStatus())).append(',')
                    .append(escape(e.getErrorCode())).append(',')
                    .append(escape(e.getErrorMessage())).append(',')
                    .append(e.getErrorRow() != null ? e.getErrorRow() : "")
                    .append('\n');
        }

        csv.append('\n');
        csv.append("section,source_issue_key,target_issue_key,status,error_message\n");
        for (MigrationIssueResult ir : issueResultService.getByJob(jobId)) {
            csv.append("issue,")
                    .append(escape(ir.getSourceIssueKey())).append(',')
                    .append(escape(ir.getTargetIssueKey())).append(',')
                    .append(escape(ir.getStatus())).append(',')
                    .append(escape(ir.getErrorMessage())).append('\n');
        }

        csv.append('\n');
        csv.append("section,file_name,source_issue_key,status,checksum\n");
        for (MigrationAttachmentResult ar : attachmentResultService.getByJob(jobId)) {
            csv.append("attachment,")
                    .append(escape(ar.getFileName())).append(',')
                    .append(escape(ar.getSourceIssueKey())).append(',')
                    .append(escape(ar.getStatus())).append(',')
                    .append(escape(ar.getChecksum())).append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String buildJobLogsText(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        List<EntityStatus> failed = entityStatusRepository.findFailedEntities(jobId);
        StringBuilder log = new StringBuilder();
        log.append("Migration Job: ").append(job.getId()).append('\n');
        log.append("Status: ").append(job.getJobStatus()).append('\n');
        log.append("Source: ").append(job.getImportSource()).append('\n');
        if (job.getErrorMessage() != null) {
            log.append("Job Error: ").append(job.getErrorMessage()).append('\n');
        }
        log.append('\n').append("Failed entities (").append(failed.size()).append("):\n");
        for (EntityStatus e : failed) {
            log.append("  [").append(e.getEntityType()).append("] ")
                    .append(e.getEntityKey());
            if (e.getErrorRow() != null) {
                log.append(" row=").append(e.getErrorRow());
            }
            log.append(" — ").append(e.getErrorCode()).append(": ").append(e.getErrorMessage()).append('\n');
        }
        return log.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
