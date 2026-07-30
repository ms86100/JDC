package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentUploadProgressReporter {

    private final MigrationJobRepository migrationJobRepository;

    @Transactional
    public void reportChunkProgress(
            UUID jobId,
            String fileName,
            int chunkIndex,
            int totalChunks,
            long bytesWritten) {
        if (jobId == null) {
            return;
        }
        migrationJobRepository.findById(jobId).ifPresent(job -> {
            Map<String, Object> meta = job.getResultMetadata() != null
                    ? new HashMap<>(job.getResultMetadata())
                    : new HashMap<>();
            meta.put("attachmentBytesWritten", bytesWritten);
            meta.put("attachmentChunkIndex", chunkIndex);
            meta.put("attachmentChunkTotal", totalChunks);
            meta.put("attachmentCurrentFile", fileName);
            meta.put("attachmentChunked", totalChunks > 1);
            job.setResultMetadata(meta);
            migrationJobRepository.save(job);
        });
    }
}
