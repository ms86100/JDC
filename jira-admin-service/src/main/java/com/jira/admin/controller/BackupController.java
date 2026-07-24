package com.jira.admin.controller;

import com.jira.admin.entity.BackupEntity;
import com.jira.admin.entity.BackupScheduleEntity;
import com.jira.admin.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
@Tag(name = "Backup & Restore", description = "System Backup and Restore API")
@CrossOrigin(origins = "*")
public class BackupController {

    private final BackupService backupService;

    @PostMapping
    @Operation(summary = "Create a new system backup")
    public ResponseEntity<BackupEntity> createBackup(@RequestBody(required = false) Map<String, String> body) {
        UUID initiatedBy = null;
        if (body != null && body.containsKey("initiatedBy")) {
            initiatedBy = UUID.fromString(body.get("initiatedBy"));
        }
        BackupEntity backup = backupService.createBackup(initiatedBy);
        return new ResponseEntity<>(backup, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get backup status")
    public ResponseEntity<BackupEntity> getBackupStatus(@PathVariable String id) {
        return backupService.getBackupStatus(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List all backups")
    public ResponseEntity<List<BackupEntity>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a backup file")
    public ResponseEntity<Map<String, Object>> downloadBackup(@PathVariable String id) {
        return backupService.getBackupStatus(id)
                .map(backup -> {
                    if (!"COMPLETED".equals(backup.getStatus())) {
                        return ResponseEntity.badRequest().<Map<String, Object>>body(
                                Map.of("error", "Backup is not ready for download"));
                    }
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "backupId", backup.getId(),
                            "filename", backup.getFilename(),
                            "fileSize", backup.getFileSize() != null ? backup.getFileSize() : 0,
                            "message", "Backup file download initiated"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore system from a backup")
    public ResponseEntity<Map<String, Object>> restoreBackup(@PathVariable String id) {
        try {
            BackupEntity backup = backupService.restoreFromBackup(id);
            return ResponseEntity.ok(Map.of(
                    "backupId", backup.getId(),
                    "status", "RESTORE_INITIATED",
                    "message", "System restore initiated from backup: " + backup.getFilename()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/schedule")
    @Operation(summary = "Configure backup schedule")
    public ResponseEntity<BackupScheduleEntity> updateSchedule(@RequestBody BackupScheduleEntity schedule) {
        return ResponseEntity.ok(backupService.updateSchedule(schedule));
    }

    @GetMapping("/schedule")
    @Operation(summary = "Get backup schedule configuration")
    public ResponseEntity<BackupScheduleEntity> getSchedule() {
        return ResponseEntity.ok(backupService.getSchedule());
    }
}
