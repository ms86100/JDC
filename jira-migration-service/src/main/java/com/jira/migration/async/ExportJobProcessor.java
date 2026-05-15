package com.jira.migration.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.entity.*;
import com.jira.migration.repository.BackupEntityRepository;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExportJobProcessor {

    private final MigrationService migrationService;
    private final BackupEntityRepository backupEntityRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final MigrationJobRepository migrationJobRepository;
    private final ObjectMapper objectMapper;

    @Async("migrationTaskExecutor")
    public CompletableFuture<String> exportProject(
            UUID jobId,
            UUID projectId,
            String exportFormat,
            Map<String, Object> options,
            UUID userId) {

        log.info("Starting project export: project={}, format={}", projectId, exportFormat);

        try {
            migrationService.markJobStarted(jobId);

            // Create export directory
            Path exportDir = Files.createTempDirectory("export-");
            Path exportFile = exportDir.resolve("export-" + projectId + "." + exportFormat);

            // Collect entities to export
            Map<String, Integer> entityCounts = new HashMap<>();
            List<EntityExportData> allEntities = new ArrayList<>();

            // Export order based on dependencies
            List<String> exportOrder = List.of(
                    "PROJECT", "ISSUE_TYPE", "STATUS", "PRIORITY", "RESOLUTION",
                    "COMPONENT", "VERSION", "WORKFLOW", "SCREEN_SCHEME",
                    "PERMISSION_SCHEME", "NOTIFICATION_SCHEME",
                    "ISSUE", "COMMENT", "ATTACHMENT", "WORKLOG", "LABEL", "CUSTOM_FIELD"
            );

            int totalEntities = 0;
            for (String entityType : exportOrder) {
                List<EntityExportData> entities = exportEntityType(projectId, entityType);
                allEntities.addAll(entities);
                entityCounts.put(entityType, entities.size());
                totalEntities += entities.size();
            }

            migrationService.setTotalEntities(jobId, totalEntities);

            // Write export file
            if ("xml".equalsIgnoreCase(exportFormat)) {
                writeXmlExport(exportFile.toString(), allEntities);
            } else if ("json".equalsIgnoreCase(exportFormat)) {
                writeJsonExport(exportFile.toString(), allEntities);
            } else if ("csv".equalsIgnoreCase(exportFormat)) {
                writeCsvExport(exportFile.toString(), allEntities);
            }

            // Create backup entries
            createBackupEntries(jobId, allEntities);

            // Mark completed
            MigrationJob job = migrationJobRepository.findById(jobId).orElseThrow();
            job.setFilePath(exportFile.toString());
            job.setResultMetadata(objectMapper.writeValueAsString(Map.of(
                    "exportFormat", exportFormat,
                    "entityCounts", entityCounts,
                    "fileSize", Files.size(exportFile)
            )));
            migrationJobRepository.save(job);

            migrationService.markJobCompleted(jobId, job.getResultMetadata());

            // Clean up temp files after delay
            cleanupTempDir(exportDir);

            return CompletableFuture.completedFuture(exportFile.toString());

        } catch (Exception e) {
            log.error("Export failed: {}", e.getMessage(), e);
            migrationService.markJobFailed(jobId, e.getMessage(), null);
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<EntityExportData> exportEntityType(UUID projectId, String entityType) {
        List<EntityExportData> entities = new ArrayList<>();

        // In production, query the appropriate service/database
        // For now, simulate with placeholder
        log.info("Exporting {} for project {}", entityType, projectId);

        return entities;
    }

    private void writeXmlExport(String filePath, List<EntityExportData> entities) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<JiraExport version=\"1.0\">\n");
        xml.append("  <ExportInfo>\n");
        xml.append("    <exportDate>").append(LocalDateTime.now()).append("</exportDate>\n");
        xml.append("    <entityCount>").append(entities.size()).append("</entityCount>\n");
        xml.append("  </ExportInfo>\n");
        xml.append("  <Entities>\n");

        for (EntityExportData entity : entities) {
            xml.append("    <Entity type=\"").append(entity.entityType).append("\">\n");
            for (Map.Entry<String, String> field : entity.fields.entrySet()) {
                xml.append("      <field name=\"").append(escapeXml(field.getKey()))
                        .append("\">").append(escapeXml(field.getValue())).append("</field>\n");
            }
            xml.append("    </Entity>\n");
        }

        xml.append("  </Entities>\n");
        xml.append("</JiraExport>\n");

        Files.writeString(Path.of(filePath), xml.toString());
    }

    private void writeJsonExport(String filePath, List<EntityExportData> entities) throws IOException {
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("exportInfo", Map.of(
                "exportDate", LocalDateTime.now().toString(),
                "entityCount", entities.size()
        ));

        Map<String, List<Map<String, String>>> entitiesByType = new HashMap<>();
        for (EntityExportData entity : entities) {
            entitiesByType.computeIfAbsent(entity.entityType, k -> new ArrayList<>())
                    .add(entity.fields);
        }
        exportData.put("entities", entitiesByType);

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), exportData);
    }

    private void writeCsvExport(String filePath, List<EntityExportData> entities) throws IOException {
        if (entities.isEmpty()) {
            Files.writeString(Path.of(filePath), "");
            return;
        }

        // Group by entity type
        Map<String, List<EntityExportData>> byType = new HashMap<>();
        for (EntityExportData entity : entities) {
            byType.computeIfAbsent(entity.entityType, k -> new ArrayList<>()).add(entity);
        }

        StringBuilder csv = new StringBuilder();

        for (Map.Entry<String, List<EntityExportData>> entry : byType.entrySet()) {
            csv.append("# Entity Type: ").append(entry.getKey()).append("\n");

            List<EntityExportData> typeEntities = entry.getValue();

            // Get all unique fields
            Set<String> allFields = new LinkedHashSet<>();
            for (EntityExportData e : typeEntities) {
                allFields.addAll(e.fields.keySet());
            }

            // Write header
            csv.append(String.join(",", allFields)).append("\n");

            // Write rows
            for (EntityExportData e : typeEntities) {
                List<String> values = new ArrayList<>();
                for (String field : allFields) {
                    values.add(escapeCsv(e.fields.getOrDefault(field, "")));
                }
                csv.append(String.join(",", values)).append("\n");
            }

            csv.append("\n");
        }

        Files.writeString(Path.of(filePath), csv.toString());
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Transactional
    void createBackupEntries(UUID jobId, List<EntityExportData> entities) {
        int sequence = 0;
        for (EntityExportData entity : entities) {
            BackupEntity backup = BackupEntity.builder()
                    .backupId(jobId)
                    .entityType(entity.entityType)
                    .entityKey(entity.entityKey)
                    .entityData(toJson(entity.fields))
                    .dependencies(toJson(entity.dependencies))
                    .parentKey(entity.parentKey)
                    .sequenceOrder(sequence++)
                    .build();
            backupEntityRepository.save(backup);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void cleanupTempDir(Path dir) {
        try {
            Thread.sleep(60000L); // Keep temp file for 1 minute for download
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        } catch (Exception e) {
            log.warn("Failed to cleanup temp dir: {}", e.getMessage());
        }
    }

    public static class EntityExportData {
        public String entityType;
        public String entityKey;
        public Map<String, String> fields;
        public List<String> dependencies;
        public String parentKey;
    }
}