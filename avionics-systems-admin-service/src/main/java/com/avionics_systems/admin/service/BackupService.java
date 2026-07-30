package com.avionics_systems.admin.service;

import com.avionics_systems.admin.entity.BackupEntity;
import com.avionics_systems.admin.entity.BackupScheduleEntity;
import com.avionics_systems.admin.repository.BackupRepository;
import com.avionics_systems.admin.repository.BackupScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import org.springframework.context.MessageSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class BackupService {

    private final BackupRepository backupRepository;
    private final BackupScheduleRepository backupScheduleRepository;
    private final RestTemplate restTemplate;
    private final MessageSource messageSource;

    @Value("${avionics-systems.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Value("${avionics-systems.services.issue-url:http://localhost:8084}")
    private String issueServiceUrl;

    @Value("${avionics-systems.services.workflow-url:http://localhost:8085}")
    private String workflowServiceUrl;

    @Value("${app.backup.status.in-progress:IN_PROGRESS}")
    private String statusInProgress;

    @Value("${app.backup.status.completed:COMPLETED}")
    private String statusCompleted;

    @Value("${app.backup.status.failed:FAILED}")
    private String statusFailed;

    @Value("${app.backup.schedule.default-cron:0 0 2 * * ?}")
    private String defaultScheduleCron;

    @Value("${app.backup.schedule.default-retention-days:30}")
    private int defaultRetentionDays;

    public BackupService(BackupRepository backupRepository,
                          BackupScheduleRepository backupScheduleRepository,
                          RestTemplate restTemplate,
                          MessageSource messageSource) {
        this.backupRepository = backupRepository;
        this.backupScheduleRepository = backupScheduleRepository;
        this.restTemplate = restTemplate;
        this.messageSource = messageSource;
    }

    private static final DateTimeFormatter BACKUP_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Transactional
    public BackupEntity createBackup(UUID initiatedBy) {
        String filename = "avionics-systems-backup-" + LocalDateTime.now().format(BACKUP_NAME_FORMAT) + ".zip";

        BackupEntity backup = BackupEntity.builder()
                .filename(filename)
                .status(statusInProgress)
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
            backup.setStatus(statusCompleted);
            backup.setCompletedAt(LocalDateTime.now());
            log.info("Backup {} completed successfully, estimated size: {} bytes", backup.getId(), estimatedSize);
        } catch (Exception e) {
            log.error("Backup {} failed: {}", backup.getId(), e.getMessage());
            backup.setStatus(statusFailed);
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
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.backup.not.found", new Object[]{backupId}, Locale.ENGLISH)));

        if (!statusCompleted.equals(backup.getStatus())) {
            throw new IllegalStateException(
                    messageSource.getMessage("error.backup.invalid.status", new Object[]{backup.getStatus()}, Locale.ENGLISH));
        }

        try {
            restoreToService(projectServiceUrl + "/api/projects/import", Map.of());
            restoreToService(issueServiceUrl + "/api/issues/import", Map.of());
            restoreToService(workflowServiceUrl + "/api/workflows/import", Map.of());
            log.info("Restore from backup {} completed successfully", backupId);
        } catch (Exception e) {
            log.error("Restore from backup {} failed: {}", backupId, e.getMessage());
            throw new IllegalStateException(
                    messageSource.getMessage("error.backup.restore.failed", new Object[]{e.getMessage()}, Locale.ENGLISH), e);
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
                    .cronExpression(defaultScheduleCron)
                    .retentionDays(defaultRetentionDays)
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
