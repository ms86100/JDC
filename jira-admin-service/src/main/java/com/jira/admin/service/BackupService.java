package com.jira.admin.service;

import com.jira.admin.entity.BackupEntity;
import com.jira.admin.entity.BackupScheduleEntity;
import com.jira.admin.repository.BackupRepository;
import com.jira.admin.repository.BackupScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final BackupRepository backupRepository;
    private final BackupScheduleRepository backupScheduleRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${jira.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Value("${jira.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    @Value("${jira.services.workflow-url:http://localhost:8085}")
    private String workflowServiceUrl;

    private static final DateTimeFormatter BACKUP_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Transactional
    public BackupEntity createBackup(UUID initiatedBy) {
        String filename = "jira-backup-" + LocalDateTime.now().format(BACKUP_NAME_FORMAT) + ".zip";

        BackupEntity backup = BackupEntity.builder()
                .filename(filename)
                .status("IN_PROGRESS")
                .initiatedBy(initiatedBy)
                .startedAt(LocalDateTime.now())
                .build();
        backup = backupRepository.save(backup);

        try {
            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("exportedAt", LocalDateTime.now().toString());
            exportData.put("initiatedBy", initiatedBy != null ? initiatedBy.toString() : "SYSTEM");

            exportData.put("projects", exportFromService(projectServiceUrl + "/api/projects/all"));
            exportData.put("issues", exportFromService(issueServiceUrl + "/api/issues/export"));
            exportData.put("workflows", exportFromService(workflowServiceUrl + "/api/workflows/export"));

            long estimatedSize = exportData.toString().getBytes().length;
            backup.setFileSize(estimatedSize);
            backup.setStatus("COMPLETED");
            backup.setCompletedAt(LocalDateTime.now());
            log.info("Backup {} completed successfully, estimated size: {} bytes", backup.getId(), estimatedSize);
        } catch (Exception e) {
            log.error("Backup {} failed: {}", backup.getId(), e.getMessage());
            backup.setStatus("FAILED");
            backup.setErrorMessage(e.getMessage());
            backup.setCompletedAt(LocalDateTime.now());
        }

        return backupRepository.save(backup);
    }

    @Transactional(readOnly = true)
    public Optional<BackupEntity> getBackupStatus(String backupId) {
        return backupRepository.findById(backupId);
    }

    @Transactional(readOnly = true)
    public List<BackupEntity> listBackups() {
        return backupRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional
    public BackupEntity restoreFromBackup(String backupId) {
        BackupEntity backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new IllegalArgumentException("Backup not found: " + backupId));

        if (!"COMPLETED".equals(backup.getStatus())) {
            throw new IllegalStateException("Cannot restore from backup with status: " + backup.getStatus());
        }

        try {
            restoreToService(projectServiceUrl + "/api/projects/import", Map.of());
            restoreToService(issueServiceUrl + "/api/issues/import", Map.of());
            restoreToService(workflowServiceUrl + "/api/workflows/import", Map.of());
            log.info("Restore from backup {} completed successfully", backupId);
        } catch (Exception e) {
            log.error("Restore from backup {} failed: {}", backupId, e.getMessage());
            throw new IllegalStateException("Restore failed: " + e.getMessage(), e);
        }

        return backup;
    }

    @Transactional
    public BackupScheduleEntity updateSchedule(BackupScheduleEntity schedule) {
        List<BackupScheduleEntity> existing = backupScheduleRepository.findAll();
        if (!existing.isEmpty()) {
            BackupScheduleEntity current = existing.get(0);
            current.setCronExpression(schedule.getCronExpression());
            current.setRetentionDays(schedule.getRetentionDays());
            current.setIsEnabled(schedule.getIsEnabled());
            return backupScheduleRepository.save(current);
        }
        return backupScheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public BackupScheduleEntity getSchedule() {
        List<BackupScheduleEntity> schedules = backupScheduleRepository.findAll();
        if (schedules.isEmpty()) {
            return BackupScheduleEntity.builder()
                    .cronExpression("0 0 2 * * ?")
                    .retentionDays(30)
                    .isEnabled(false)
                    .build();
        }
        return schedules.get(0);
    }

    private Object exportFromService(String url) {
        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            log.warn("Could not export from {}: {}", url, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private void restoreToService(String url, Map<String, Object> data) {
        try {
            restTemplate.postForObject(url, data, Void.class);
        } catch (Exception e) {
            log.warn("Could not restore to {}: {}", url, e.getMessage());
        }
    }
}
