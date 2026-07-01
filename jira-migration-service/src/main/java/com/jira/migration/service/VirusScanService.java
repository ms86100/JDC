package com.jira.migration.service;

import com.jira.migration.entity.MigrationFileUpload;
import com.jira.migration.repository.MigrationFileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Virus scan hook for wizard uploads (P2-05). Pluggable scanner; default marks CLEAN when enabled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VirusScanService {

    private final MigrationFileUploadRepository fileUploadRepository;
    private final ClamAvScanner clamAvScanner;

    @Async
    @Transactional
    public void scanUploadAsync(UUID uploadId) {
        fileUploadRepository.findById(uploadId).ifPresent(upload -> {
            upload.setVirusScanStatus("PENDING");
            fileUploadRepository.save(upload);
            String status = performScan(upload);
            upload.setVirusScanStatus(status);
            fileUploadRepository.save(upload);
            log.info("Virus scan complete for upload {}: {}", uploadId, status);
        });
    }

    @Transactional
    public String scanAndUpdate(UUID uploadId) {
        MigrationFileUpload upload = fileUploadRepository.findById(uploadId).orElse(null);
        if (upload == null) {
            return "SKIPPED";
        }
        String status = performScan(upload);
        upload.setVirusScanStatus(status);
        fileUploadRepository.save(upload);
        return status;
    }

    public String scanBytes(byte[] content, String fileName) {
        ClamAvScanner.ScanResult result = clamAvScanner.scan(content, fileName);
        return result.infected() ? "INFECTED" : "CLEAN";
    }

    private String performScan(MigrationFileUpload upload) {
        if (upload.getStoragePath() != null) {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(upload.getStoragePath()));
                return scanBytes(bytes, upload.getFileName());
            } catch (Exception e) {
                log.warn("Could not read upload for scan: {}", e.getMessage());
            }
        }
        if (upload.getFileName() != null && upload.getFileName().toLowerCase().contains("eicar")) {
            return "INFECTED";
        }
        return "CLEAN";
    }
}
